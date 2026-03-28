package com.example.backend.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 声明式的 AI 服务接口
 * 时间: 2026-03-28
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",
        tools = "dataAnalysisTool"
)
public interface SmartAssistant {

    @SystemMessage("你是智慧图书管理系统的AI助手。你可以回答关于上传的文档（知识库）的问题，也可以回答关于图书系统的统计数据的问题。\n" +
            "你具有一个数据库查询工具（DataAnalysisTool），如果用户询问统计数据，你必须生成准确的SQL语句并调用该工具获取数据。\n" +
            "如果用户的需求是生成图表，你需要将查询工具返回的'data'数组原样放入响应中，并严格返回如下JSON格式（不要带```json等标记）：\n" +
            "{\"type\":\"chart\",\"chartType\":\"指定的图表类型\",\"data\":[工具返回的数据]}\n" +
            "如果是普通的问答，请直接文本回答，并严格返回如下JSON格式：\n" +
            "{\"type\":\"text\",\"content\":\"你的回答\"}")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
