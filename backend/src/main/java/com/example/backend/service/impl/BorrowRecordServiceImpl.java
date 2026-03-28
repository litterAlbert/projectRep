package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.BorrowRecordMapper;
import com.example.backend.pojo.BorrowRecord;
import com.example.backend.service.BorrowRecordService;
import org.springframework.stereotype.Service;

@Service
public class BorrowRecordServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {
}
