import com.sysco.Application;
import com.sysco.producer.Sender;
import example.avro.User;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EmbeddedKafka(partitions = 1, controlledShutdown = false, topics = {"${topic.name}"})
@SpringBootTest(classes = {Application.class, TestProducer.class, TestProducer.TestConfig.class})
@ActiveProfiles("test")
public class TestProducer {
  @Autowired
  EmbeddedKafkaBroker embeddedKafkaBroker;
  @Autowired
  Sender sender;
  @Value(value = "${topic.name}")
  private String topicName;

  @Configuration
  static class TestConfig {
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    public ProducerFactory<String, User> producerFactory() {
      Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
      return new DefaultKafkaProducerFactory<>(producerProps);
    }

    @Bean
    public KafkaTemplate<String, User> kafkaTemplate() {
      return new KafkaTemplate<>(producerFactory());
    }
  }

  @Test
  void testKafkaListener() {
    Consumer<String, String> consumer = configureConsumer();
    User user = mockUser();
    sender.send(topicName, user);
    ConsumerRecord<String, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, topicName);
    System.out.println(singleRecord.value());
  }

  private User mockUser() {
    return User.newBuilder().setName("mock-name").setFavoriteColor("green").setFavoriteNumber(9).build();
  }

  private Consumer<String, String> configureConsumer() {
    Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
    consumer.subscribe(Collections.singleton(topicName));
    return consumer;
  }
}
