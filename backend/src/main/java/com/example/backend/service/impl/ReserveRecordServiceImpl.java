package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.ReserveRecordMapper;
import com.example.backend.pojo.ReserveRecord;
import com.example.backend.service.ReserveRecordService;
import org.springframework.stereotype.Service;

@Service
public class ReserveRecordServiceImpl extends ServiceImpl<ReserveRecordMapper, ReserveRecord> implements ReserveRecordService {
}
