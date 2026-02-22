# CLAUDE.md

This file provides guidance when working with code in this repository.

## Build Commands

```bash
./gradlew build          # Build plugin JAR (output: build/libs/)
./gradlew clean build    # Clean and rebuild
./gradlew test           # Run tests
./gradlew test --tests "com.plugin.anthrophicquery.AnthrophicquerySpec"  # Single test
```

## Architecture

**Type**: Rundeck WorkflowStep plugin (Groovy 3.0.9, Java 11, Rundeck SDK 5.0.2)

### Source Structure
```
src/main/groovy/com/plugin/anthrophicquery/
  Anthrophicquery.groovy  # Main plugin - @Plugin, @PluginProperty annotations
  Util.groovy             # Key Storage helper
  Constants.groovy        # API constants
  FailureReason.groovy    # Custom failure enum
```

### Plugin Properties
Defined via `@PluginProperty` with `@RenderingOptions` for UI grouping:
- **API Configuration**: `apiUrl`, `apiKeyPath`, `model`
- **Query Configuration**: `prompt`
- **Advanced**: `maxTokens`, `temperature`, `topP`, `systemMessage`

### Output Context
Plugin exports to downstream steps:
- `anthropic.response` - Full JSON
- `anthropic.content` - Extracted text

## Testing

Spock Framework 2.2. Mock `PluginStepContext` and `ExecutionContext`. The existing test file needs updating to match current implementation.

## Deployment

Copy built JAR to `$RDECK_BASE/libext/` and restart Rundeck.
