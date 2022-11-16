package com.sysco.producer;

import example.avro.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Service
public class Sender {
  @Autowired
  private KafkaTemplate<String, User> kafkaTemplate;

  public void send(String topic,User data) {
    kafkaTemplate.send(topic, data);
  }
}
