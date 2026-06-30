package com.project.chat.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GenerateRequest {

    private String model;
    private String prompt;
    private boolean stream;
    private Options options;

    public GenerateRequest() {
    }

    public GenerateRequest(String model, String prompt, boolean stream, double temperature, int maxTokens) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.options = new Options(temperature, maxTokens);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Options {
        private double temperature;
        @JsonProperty("num_predict")
        private int numPredict;

        public Options() {
        }

        public Options(double temperature, int numPredict) {
            this.temperature = temperature;
            this.numPredict = numPredict;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getNumPredict() {
            return numPredict;
        }

        public void setNumPredict(int numPredict) {
            this.numPredict = numPredict;
        }
    }
}
