package com.example.spring.axon.reactor;

import com.thoughtworks.xstream.XStream;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AxonConfig {

  @Bean
  public XStream xStream() {
    XStream xStream = new XStream();

    xStream.allowTypesByWildcard(new String[] {"com.example.**"});
    return xStream;
  }

  /**
   * By default, we want the XStreamSerializer
   *
   * @param xStream Injects XStream
   * @return Serializer
   */
  @Bean
  public Serializer defaultSerializer(XStream xStream) {
    // Set the secure types on the xStream instance
    return XStreamSerializer.builder().xStream(xStream).build();
  }

  /**
   * But for all our messages we'd prefer the JacksonSerializer due to JSON's smaller format
   *
   * @return Serializer
   */
  @Bean
  @Primary
  @Qualifier("messageSerializer")
  public Serializer messageSerializer() {
    return JacksonSerializer.builder().lenientDeserialization().defaultTyping().build();
  }
}
