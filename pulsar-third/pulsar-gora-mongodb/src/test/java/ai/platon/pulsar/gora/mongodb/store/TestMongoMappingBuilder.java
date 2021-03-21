/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.gora.mongodb.store;

import org.apache.gora.examples.generated.WebPage;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestMongoMappingBuilder {

  @Test
  public void testMultiMapping_Webpage() throws IOException {
    MongoStore<String, WebPage> store = new MongoStore<>();
    store.setKeyClass(String.class);
    store.setPersistentClass(WebPage.class);
    MongoMappingBuilder<String, WebPage> builder = new MongoMappingBuilder<>(
        store);
    builder.fromFile("/multimapping.xml");
    MongoMapping mapping = builder.build();

    // Check collection name
    assertEquals("frontier", mapping.getCollectionName());
    mapping.renameCollection("frontier", "newNameForFrontier");
    assertEquals("newNameForFrontier", mapping.getCollectionName());

    // Check field names
    assertEquals("baseUrl", mapping.getDocumentField("baseUrl"));
    assertEquals("status", mapping.getDocumentField("status"));
    assertEquals("fetchTime", mapping.getDocumentField("fetchTime"));
    assertEquals("prevFetchTime", mapping.getDocumentField("prevFetchTime"));
    assertEquals("fetchInterval", mapping.getDocumentField("fetchInterval"));
    assertEquals("retriesSinceFetch",
        mapping.getDocumentField("retriesSinceFetch"));
    assertEquals("modifiedTime", mapping.getDocumentField("modifiedTime"));
    assertEquals("protocolStatus", mapping.getDocumentField("protocolStatus"));
    assertEquals("content", mapping.getDocumentField("content"));
    assertEquals("contentType", mapping.getDocumentField("contentType"));
    assertEquals("prevSignature", mapping.getDocumentField("prevSignature"));
    assertEquals("title", mapping.getDocumentField("title"));
    assertEquals("text", mapping.getDocumentField("text"));
    assertEquals("parseStatus", mapping.getDocumentField("parseStatus"));
    assertEquals("score", mapping.getDocumentField("score"));
    assertEquals("reprUrl", mapping.getDocumentField("reprUrl"));
    assertEquals("headers", mapping.getDocumentField("headers"));
    assertEquals("outlinks", mapping.getDocumentField("outlinks"));
    assertEquals("inlinks", mapping.getDocumentField("inlinks"));
    assertEquals("markers", mapping.getDocumentField("markers"));
    assertEquals("metadata", mapping.getDocumentField("metadata"));

    // Check field types
    Assert.assertEquals(MongoMapping.DocumentFieldType.STRING,
        mapping.getDocumentFieldType("baseUrl"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT32,
        mapping.getDocumentFieldType("status"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT64,
        mapping.getDocumentFieldType("fetchTime"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT64,
        mapping.getDocumentFieldType("prevFetchTime"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT32,
        mapping.getDocumentFieldType("fetchInterval"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT32,
        mapping.getDocumentFieldType("retriesSinceFetch"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.INT64,
        mapping.getDocumentFieldType("modifiedTime"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("protocolStatus"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.BINARY,
        mapping.getDocumentFieldType("content"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.STRING,
        mapping.getDocumentFieldType("contentType"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.BINARY,
        mapping.getDocumentFieldType("prevSignature"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.STRING,
        mapping.getDocumentFieldType("title"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.STRING, mapping.getDocumentFieldType("text"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("parseStatus"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOUBLE,
        mapping.getDocumentFieldType("score"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.STRING,
        mapping.getDocumentFieldType("reprUrl"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("headers"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("outlinks"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("inlinks"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("markers"));
    Assert.assertEquals(MongoMapping.DocumentFieldType.DOCUMENT,
        mapping.getDocumentFieldType("metadata"));
  }
}