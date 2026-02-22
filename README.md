<p align="center">
  <img src="src/main/resources/resources/icon.png" alt="Anthropic Plugin" width="80"/>
</p>

<h1 align="center">Rundeck Anthropic Query Plugin</h1>

<p align="center">
  <strong>Integrate Anthropic AI models into your Rundeck automation workflows</strong>
</p>

<p align="center">
  <a href="#installation">Installation</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#usage">Usage</a> •
  <a href="#examples">Examples</a> •
  <a href="#support">Support</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Rundeck-Community-5C9E3D?logo=rundeck&logoColor=white" alt="Rundeck Community"/>
  <img src="https://img.shields.io/badge/Runbook_Automation-Self_Hosted-0F1E57?logo=pagerduty&logoColor=white" alt="Runbook Automation Self-Hosted"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License"/>
</p>

---

## Overview

This plugin adds an **Anthropic Query** workflow step to Rundeck, enabling you to leverage AI-powered automation in your runbooks.

**Key Features:**
- Send prompts to Anthropic AI models directly from workflow steps
- Use AI-generated responses in subsequent job steps
- Fine-tune model behavior with temperature, token limits, and system messages
- Secure credential management via Rundeck Key Storage

## Compatibility

| Platform | Version |
|----------|---------|
| Rundeck Community | 3.x, 4.x, 5.x |
| Runbook Automation (Self-Hosted) | 4.x, 5.x |

## Installation

### Recommended: Via UI

1. Download the latest JAR from [Releases](../../releases)
2. In Rundeck, navigate to **System Menu** → **Plugins** → **Upload Plugin**
3. Select the downloaded JAR file
4. The plugin is immediately available—no restart required

### Alternative: Manual Installation

```bash
cp anthrophic-query-*.jar $RDECK_BASE/libext/
```
Restart Rundeck after manual installation.

## Configuration

### 1. Store Your API Key

Store your Anthropic API key securely in Rundeck Key Storage:

1. Navigate to **Project Settings** → **Key Storage**
2. Click **Add or Upload a Key**
3. Select **Password** as the key type
4. Paste your Anthropic API key
5. Set a path: `keys/project/anthropic/api_key`

### 2. Plugin Parameters

| Parameter | Required | Default | Description |
|-----------|:--------:|---------|-------------|
| **API Key Path** | ✓ | — | Path to API key in Rundeck Key Storage |
| **Prompt** | ✓ | — | The prompt to send to the AI model |
| **API URL** | | `https://api.anthropic.com` | Anthropic API endpoint |
| **Model** | | `claude-3-opus-20240229` | Model identifier |
| **Max Tokens** | | `1024` | Maximum response length |
| **Temperature** | | — | Response randomness (0.0–1.0) |
| **Top P** | | — | Nucleus sampling threshold |
| **System Message** | | — | System context for the model |

> **Note:** Temperature and Top P are mutually exclusive—specify only one.

## Usage

### Adding to a Job

1. Create or edit a Rundeck job
2. Add a workflow step → Select **Anthropic Query**
3. Configure the required parameters
4. Save and run

### Output Variables

The plugin exports data for use in subsequent steps:

| Variable | Description |
|----------|-------------|
| `${data.anthropic_response}` | Full JSON response from API |
| `${data.anthropic_content}` | Extracted text content |

### Using with Log Filters

Extract specific fields using the **JSON Mapper** log filter:

1. Add a log filter to the Anthropic Query step
2. Select **json-mapper**
3. Configure:
   - **Filter:** `.content`
   - **Prefix:** `result`
4. Access in later steps: `${data.result}`

## Examples

### Basic Translation

```json
{
  "name": "AI Translation",
  "options": [
    { "name": "text", "label": "Text to translate" }
  ],
  "sequence": {
    "commands": [
      {
        "type": "anthrophic-query",
        "configuration": {
          "apiKeyPath": "keys/project/anthropic/api_key",
          "prompt": "Translate to French: ${option.text}",
          "systemMessage": "Respond with plain text only."
        }
      },
      {
        "exec": "echo \"Translation: ${data.anthropic_content}\""
      }
    ]
  }
}
```

### Log Analysis

```json
{
  "type": "anthrophic-query",
  "configuration": {
    "apiKeyPath": "keys/project/anthropic/api_key",
    "model": "claude-3-haiku-20240307",
    "maxTokens": "2048",
    "prompt": "Analyze this error log and suggest fixes:\n\n${data.errorLog}",
    "systemMessage": "You are a DevOps engineer. Be concise."
  }
}
```

### Incident Summary

```json
{
  "type": "anthrophic-query",
  "configuration": {
    "apiKeyPath": "keys/project/anthropic/api_key",
    "model": "claude-3-opus-20240229",
    "prompt": "Summarize this incident for a status page:\n\n${data.incidentDetails}",
    "temperature": "0.3"
  }
}
```

## Available Models

| Model | Best For |
|-------|----------|
| `claude-3-opus-20240229` | Complex analysis, nuanced tasks |
| `claude-3-sonnet-20240229` | Balanced performance and speed |
| `claude-3-haiku-20240307` | Fast responses, simple tasks |

See [Anthropic Documentation](https://docs.anthropic.com/en/docs/about-claude/models) for the complete model list.

## Building from Source

### Requirements

- Java 11+
- Gradle 7.x (wrapper included)

### Commands

```bash
./gradlew build           # Build JAR
./gradlew clean build     # Clean and rebuild
./gradlew test            # Run tests
```

Output: `build/libs/anthrophic-query-*.jar`

## Support

- **Issues:** [GitHub Issues](../../issues)
- **Pull Requests:** Contributions welcome!

> **Disclaimer:** This plugin is provided "as is" without warranty of any kind, express or implied. Use at your own risk. See [LICENSE](LICENSE) for details.

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <sub>Built by <a href="https://github.com/rundecktoolkit">rundecktoolkit</a></sub>
</p>
