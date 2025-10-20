package net.ravendb.client.documents.operations.AI.agents.config;

public class AiAgentSummarizationByTokens {
    private String summarizationTaskBeginningPrompt; // Optional
    private String summarizationTaskEndPrompt;       // Optional
    private String resultPrefix;                     // Optional
    private Integer maxTokensBeforeSummarization;    // Optional
    private Integer maxTokensAfterSummarization;     // Optional

    public AiAgentSummarizationByTokens() {
        // Default constructor
    }

    public AiAgentSummarizationByTokens(String summarizationTaskBeginningPrompt,
                                        String summarizationTaskEndPrompt,
                                        String resultPrefix,
                                        Integer maxTokensBeforeSummarization,
                                        Integer maxTokensAfterSummarization) {
        this.summarizationTaskBeginningPrompt = summarizationTaskBeginningPrompt;
        this.summarizationTaskEndPrompt = summarizationTaskEndPrompt;
        this.resultPrefix = resultPrefix;
        this.maxTokensBeforeSummarization = maxTokensBeforeSummarization;
        this.maxTokensAfterSummarization = maxTokensAfterSummarization;
    }

    public String getSummarizationTaskBeginningPrompt() {
        return summarizationTaskBeginningPrompt;
    }

    public void setSummarizationTaskBeginningPrompt(String summarizationTaskBeginningPrompt) {
        this.summarizationTaskBeginningPrompt = summarizationTaskBeginningPrompt;
    }

    public String getSummarizationTaskEndPrompt() {
        return summarizationTaskEndPrompt;
    }

    public void setSummarizationTaskEndPrompt(String summarizationTaskEndPrompt) {
        this.summarizationTaskEndPrompt = summarizationTaskEndPrompt;
    }

    public String getResultPrefix() {
        return resultPrefix;
    }

    public void setResultPrefix(String resultPrefix) {
        this.resultPrefix = resultPrefix;
    }

    public Integer getMaxTokensBeforeSummarization() {
        return maxTokensBeforeSummarization;
    }

    public void setMaxTokensBeforeSummarization(Integer maxTokensBeforeSummarization) {
        this.maxTokensBeforeSummarization = maxTokensBeforeSummarization;
    }

    public Integer getMaxTokensAfterSummarization() {
        return maxTokensAfterSummarization;
    }

    public void setMaxTokensAfterSummarization(Integer maxTokensAfterSummarization) {
        this.maxTokensAfterSummarization = maxTokensAfterSummarization;
    }
}
