/*
 * #%L
 * llamara-backend
 * %%
 * Copyright (C) 2024 - 2025 Contributors to the LLAMARA project
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.llamara.ai.internal.chat.aiservice;

import java.util.UUID;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Interface used to define an <a href="https://docs.langchain4j.dev/tutorials/ai-services/">AI
 * Service</a>.
 *
 * <p>A LangChain4j AI service is used to hide complexities of interacting with LLMs and other
 * components behind a simple API by declaratively defining an interface with the desired API.
 * LangChain4j then provides an object (proxy) that implements this interface. You can think of AI
 * service as a component of the service layer of your application.
 *
 * @author Florian Hotze - Initial contribution
 */
public interface ChatModelAiService {
    String SYSTEM_MESSAGE =
            """
You are LLAMARA, the Large Language Assistant for Model Augmented Retrieval and Analysis:
  - Your goal is to support your users in finding and working with the information they need from the user-provided knowledge base.
  - Focus on facts and avoid giving your opinion or interpretation on any matter.
  - Ask for clarification if you are unsure about the user's request.
  - Always answer in the same language as the user's input.
  - If you are unable to answer a question, inform the user about it.
""";

    @SystemMessage(SYSTEM_MESSAGE)
    Result<String> chat(@MemoryId UUID sessionId, boolean history, @UserMessage String prompt);

    Result<String> chatWithoutSystemMessage(
            @MemoryId UUID sessionId, boolean history, @UserMessage String prompt);

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream chatAndStreamResponse(
            @MemoryId UUID sessionId, boolean history, @UserMessage String prompt);

    /**
     * Clean the given text by removing unnecessary noise and formatting it.
     *
     * @param text the text to clean
     * @return the cleaned text
     */
    @UserMessage(
            """
Your task is to process the text delimited by <text> and </text> in the following way:

Take the text and perform the following clean-up steps on it:
    1. Remove all unnecessary noise from it.
       Noise is ONLY: page numbers, references, sources, bibliography, table of content, page headers and page footers.
    2. Format the resulting text.
       You MUST keep the original headings, paragraphs, and lists.

You MUST NOT:
    - Add anything to the text.
    - Remove anything that is no noise.
    - Summarize the text.

Answer ONLY with the cleaned text formatted as text only, i.e. NO Markdown, HTML, or similar.

<text>
  {text}
</text>
""")
    String clean(String text);
}
