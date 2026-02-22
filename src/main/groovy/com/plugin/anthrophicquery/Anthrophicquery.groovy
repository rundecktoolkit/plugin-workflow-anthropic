package com.plugin.anthrophicquery;

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.core.execution.ExecutionListener
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.rundeck.storage.api.StorageException
import java.net.URL
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection

@Plugin(name = PLUGIN_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = PLUGIN_TITLE, description = PLUGIN_DESCRIPTION)
class Anthrophicquery implements StepPlugin {
    public static final String PLUGIN_NAME = "anthrophic-query"
    public static final String PLUGIN_TITLE = "Anthropic Query"
    public static final String PLUGIN_DESCRIPTION = "Query Anthropic AI models with custom prompts and get JSON responses."

    Map<String, String> meta = Collections.singletonMap("content-data-type", "application/json")

    @PluginProperty(
        title = "API URL",
        description = "The Anthropic API base URL. Leave blank to use the default Anthropic API endpoint.",
        defaultValue = "https://api.anthropic.com",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "groupName", value = "API Configuration")
    ])
    String apiUrl

    @PluginProperty(
        title = "API Key Path",
        description = "REQUIRED: The path to the Key Storage entry for your Anthropic API Key.",
        required = true
    )
    @RenderingOptions([
        @RenderingOption(key = "selectionAccessor", value = "STORAGE_PATH"),
        @RenderingOption(key = "storagePathRoot", value = "keys"),
        @RenderingOption(key = "storageFileMetaFilter", value = "Rundeck-data-type=password"),
        @RenderingOption(key = "groupName", value = "API Configuration")
    ])
    String apiKeyPath

    @PluginProperty(
        title = "Model",
        description = "The Anthropic model to use for the query.",
        defaultValue = "claude-3-opus-20240229",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "groupName", value = "API Configuration")
    ])
    String model

    @PluginProperty(
        title = "Prompt",
        description = "REQUIRED: The prompt to send to the Anthropic API.",
        required = true
    )
    @RenderingOptions([
        @RenderingOption(key = "displayType", value = "MULTI_LINE"),
        @RenderingOption(key = "groupName", value = "Query Configuration")
    ])
    String prompt

    @PluginProperty(
        title = "Max Tokens",
        description = "Maximum number of tokens to generate in the response.",
        defaultValue = "1024",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "groupName", value = "Advanced Configuration")
    ])
    String maxTokens

    @PluginProperty(
        title = "Temperature",
        description = "Controls randomness in the response (0.0 to 1.0). Leave blank to use model default. Cannot be used with Top P.",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "groupName", value = "Advanced Configuration")
    ])
    String temperature

    @PluginProperty(
        title = "Top P",
        description = "Controls diversity via nucleus sampling (0.0 to 1.0). Leave blank to use model default. Cannot be used with Temperature.",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "groupName", value = "Advanced Configuration")
    ])
    String topP

    @PluginProperty(
        title = "System Message",
        description = "Optional system message to set the context for the AI assistant.",
        required = false
    )
    @RenderingOptions([
        @RenderingOption(key = "displayType", value = "MULTI_LINE"),
        @RenderingOption(key = "groupName", value = "Advanced Configuration")
    ])
    String systemMessage

    @Override
    void executeStep(final PluginStepContext context, final Map<String, Object> configuration) {
        ExecutionListener logger = context.getExecutionContext().getExecutionListener()
        
        // Validate required fields
        if (!prompt || prompt.trim().isEmpty()) {
            throw new StepException("Prompt is required", StepFailureReason.ConfigurationFailure)
        }
        
        if (!apiKeyPath || apiKeyPath.trim().isEmpty()) {
            throw new StepException("API Key Path is required", StepFailureReason.ConfigurationFailure)
        }

        // Get API key from storage
        String apiKey
        try {
            apiKey = Util.getPasswordFromKeyStorage(apiKeyPath, context)
        } catch (StorageException e) {
            throw new StepException("Error accessing API key at ${apiKeyPath}: ${e.getMessage()}", e, StepFailureReason.ConfigurationFailure)
        }

        // Set defaults
        String finalApiUrl = apiUrl ?: "https://api.anthropic.com"
        String finalModel = model ?: "claude-3-opus-20240229"
        String finalMaxTokens = maxTokens ?: "1024"

        // Validate API URL uses HTTPS
        if (!finalApiUrl.startsWith("https://")) {
            throw new StepException("API URL must use HTTPS", StepFailureReason.ConfigurationFailure)
        }

        // Validate temperature and topP are not both specified
        boolean hasTemperature = temperature && !temperature.trim().isEmpty()
        boolean hasTopP = topP && !topP.trim().isEmpty()
        
        if (hasTemperature && hasTopP) {
            throw new StepException("Temperature and Top P cannot both be specified. Please use only one.", StepFailureReason.ConfigurationFailure)
        }

        logger.log(3, "Making request to Anthropic API")
        logger.log(3, "Model: ${finalModel}")
        logger.log(3, "Using API key: ****${apiKey?.takeRight(4) ?: 'none'}")

        try {
            String response = makeAnthropicRequestInternal(finalApiUrl, apiKey, finalModel, prompt, finalMaxTokens, temperature, topP, systemMessage)
            
            // Parse the response to extract just the content
            def jsonSlurper = new JsonSlurper()
            def responseData = jsonSlurper.parseText(response)
            
            // Create clean output structure
            def outputData = [
                model: responseData.model,
                content: responseData.content,
                usage: responseData.usage,
                stop_reason: responseData.stop_reason
            ]
            
            String jsonOutput = JsonOutput.toJson(outputData)
            
            // Add to output context
            context.getExecutionContext().getOutputContext().addOutput("anthropic", "response", jsonOutput)
            context.getExecutionContext().getOutputContext().addOutput("anthropic", "content", responseData.content[0].text)
            
            // Log the result as pure JSON
            logger.log(2, jsonOutput, meta)
            
        } catch (Exception e) {
            logger.log(0, "Anthropic API request failed: ${e.class.simpleName}")
            throw new StepException("Failed to get response from Anthropic API: ${e.getMessage()}", e, StepFailureReason.IOFailure)
        }
    }

    private String makeAnthropicRequestInternal(String baseUrl, String apiKey, String model, String prompt, 
                                              String maxTokens, String temperature, String topP, String systemMessage) {
        String endpoint = "${baseUrl}/v1/messages"
        URL url = new URL(endpoint)
        HttpsURLConnection connection = url.openConnection() as HttpsURLConnection
        
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("x-api-key", apiKey)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
        connection.setDoOutput(true)
        connection.setConnectTimeout(30000)
        connection.setReadTimeout(60000)

        // Build request body
        def requestBody = [
            model: model,
            max_tokens: Integer.parseInt(maxTokens),
            messages: [
                [
                    role: "user",
                    content: prompt
                ]
            ]
        ]

        // Add system message if provided
        if (systemMessage && !systemMessage.trim().isEmpty()) {
            requestBody.system = systemMessage.trim()
        }

        // Add temperature if provided and not empty
        if (temperature && !temperature.trim().isEmpty()) {
            try {
                requestBody.temperature = Double.parseDouble(temperature.trim())
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid temperature value: ${temperature}")
            }
        }

        // Add top_p if provided and not empty
        if (topP && !topP.trim().isEmpty()) {
            try {
                requestBody.top_p = Double.parseDouble(topP.trim())
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid top_p value: ${topP}")
            }
        }

        String jsonRequest = JsonOutput.toJson(requestBody)
        
        connection.outputStream.withWriter("UTF-8") { writer ->
            writer.write(jsonRequest)
        }

        int responseCode = connection.responseCode
        
        if (responseCode == 200) {
            return connection.inputStream.text
        } else {
            String errorResponse = connection.errorStream?.text ?: "No error details available"
            throw new IOException("HTTP ${responseCode}: ${errorResponse}")
        }
    }
}