package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.ChatSessionMapper;
import com.example.backend.pojo.ChatSession;
import com.example.backend.service.ChatSessionService;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {
}
