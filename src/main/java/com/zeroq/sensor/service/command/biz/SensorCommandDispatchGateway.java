package com.zeroq.sensor.service.command.biz;

import com.zeroq.sensor.service.command.vo.SensorCommandDispatchMessage;

public interface SensorCommandDispatchGateway {
    void dispatch(SensorCommandDispatchMessage message);
}
