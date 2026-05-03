package com.artinus.subscription.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;

public class OpenAiResponseTextExtractor {

	public String extract(JsonNode response) {
		if (response == null) {
			return "";
		}
		JsonNode outputText = response.get("output_text");
		if (outputText != null && outputText.isTextual()) {
			return outputText.asText().trim();
		}
		JsonNode output = response.get("output");
		if (output == null || !output.isArray()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (JsonNode item : output) {
			appendContentText(builder, item.get("content"));
		}
		return builder.toString().trim();
	}

	private void appendContentText(StringBuilder builder, JsonNode content) {
		if (content == null || !content.isArray()) {
			return;
		}
		for (JsonNode contentItem : content) {
			JsonNode text = contentItem.get("text");
			if (text != null && text.isTextual()) {
				builder.append(text.asText());
			}
		}
	}
}
