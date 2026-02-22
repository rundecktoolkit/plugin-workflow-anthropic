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
  <a href="#building">Building</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Rundeck-3.x%20%7C%204.x%20%7C%205.x-blue" alt="Rundeck Compatibility"/>
  <img src="https://img.shields.io/badge/Java-11%2B-orange" alt="Java 11+"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License"/>
</p>

---

## Overview

This plugin adds an **Anthropic Query** workflow step to Rundeck, enabling you to:

- Send prompts to Anthropic AI models directly from your automation jobs
- Use AI-generated responses in subsequent workflow steps
- Configure model parameters for fine-tuned control
- Securely manage API credentials via Rundeck Key Storage

## Installation

### From Release

1. Download the latest JAR from [Releases](../../releases)
2. Copy to your Rundeck server:
   ```bash
   cp anthrophic-query-*.jar $RDECK_BASE/libext/
   ```
3. Restart Rundeck or reload plugins

### From Source

```bash
./gradlew build
cp build/libs/anthrophic-query-*.jar $RDECK_BASE/libext/
```

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

### Basic Translation Job

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

### Code Review

```json
{
  "type": "anthrophic-query",
  "configuration": {
    "apiKeyPath": "keys/project/anthropic/api_key",
    "model": "claude-3-opus-20240229",
    "prompt": "Review this code for security issues:\n\n${data.codeContent}",
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

## Building

### Requirements

- Java 11+
- Gradle 7.x (wrapper included)

### Commands

```bash
# Build
./gradlew build

# Clean build
./gradlew clean build

# Run tests
./gradlew test
```

The built JAR is output to `build/libs/`.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <sub>Built by <a href="https://github.com/rundecktoolkit">rundecktoolkit</a></sub>
</p>
