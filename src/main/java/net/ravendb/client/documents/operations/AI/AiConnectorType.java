package net.ravendb.client.documents.operations.AI;

/**
 * Supported connector types for AI agents.
 */
public enum AiConnectorType {
    None,
    OpenAi,
    AzureOpenAi,
    Ollama,
    Embedded,
    Google,
    HuggingFace,
    MistralAi,
    Vertex
}
