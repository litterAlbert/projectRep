package com.example.backend.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 声明式的 AI 服务接口
 * 时间: 2026-03-29
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",//向量数据库检索对象
        tools = "dataAnalysisTool"
)
public interface SmartAssistant {

    @SystemMessage("你是智慧图书管理系统的AI助手。你可以回答关于上传的文档（知识库）的问题，也可以回答关于图书系统的统计数据的问题。\n" +
            "你具有一个数据库查询工具（DataAnalysisTool），如果用户询问统计数据，你必须生成准确的SQL语句并调用该工具获取数据。\n" +
            "如果用户的需求是生成图表，你需要将查询工具返回的'data'数组原样放入响应中（确保计数字段的 key 统一为 'value'），并严格返回如下JSON格式（不要带```json等标记）：\n" +
            "{\"type\":\"chart\",\"chartType\":\"指定的图表类型\",\"data\":[工具返回的数据，其中计数相关的 key 请转为 'value']}\n" +
            "如果你的回答主要是基于提供的文档知识（即 RAG 检索到的内容），请严格返回如下JSON格式，并将参考的文档来源放在 source_nodes 数组中：\n" +
            "{\"type\":\"doc\",\"content\":\"你的回答\",\"source_nodes\":[\"来源文档名1\", \"来源文档名2\"]}\n" +
            "如果只是普通问答，不涉及文档知识，请直接文本回答，并严格返回如下JSON格式：\n" +
            "{\"type\":\"text\",\"content\":\"你的回答\"}")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
