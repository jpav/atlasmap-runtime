/**
 * Copyright (C) 2017 Red Hat, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.atlasmap.xml.module;

import io.atlasmap.core.AtlasMappingUtil;
import io.atlasmap.core.DefaultAtlasConversionService;
import io.atlasmap.spi.AtlasModuleDetail;
import io.atlasmap.spi.AtlasModuleMode;
import io.atlasmap.v2.AtlasMapping;
import io.atlasmap.v2.AtlasModelFactory;
import io.atlasmap.v2.DataSource;
import io.atlasmap.v2.DataSourceType;
import io.atlasmap.v2.FieldType;
import io.atlasmap.v2.Mapping;
import io.atlasmap.v2.MappingType;
import io.atlasmap.v2.MockField;
import io.atlasmap.v2.Validation;
import io.atlasmap.v2.ValidationStatus;
import io.atlasmap.validators.AtlasValidationTestHelper;
import io.atlasmap.xml.v2.AtlasXmlModelFactory;
import io.atlasmap.xml.v2.XmlComplexType;
import io.atlasmap.xml.v2.XmlField;
import io.atlasmap.xml.v2.XmlFields;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class XmlValidationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(XmlValidationServiceTest.class);
    protected io.atlasmap.xml.v2.ObjectFactory xmlModelFactory = null;
    protected AtlasMappingUtil mappingUtil = null;
    protected XmlValidationService sourceValidationService = null;
    protected XmlValidationService targetValidationService = null;
    protected AtlasValidationTestHelper validationHelper = null;
    protected List<Validation> validations = null;
    protected AtlasModuleDetail moduleDetail = null;

    @Before
    public void setUp() {
        xmlModelFactory = new io.atlasmap.xml.v2.ObjectFactory();
        mappingUtil = new AtlasMappingUtil("io.atlasmap.v2:io.atlasmap.xml.v2");
        moduleDetail = XmlModule.class.getAnnotation(AtlasModuleDetail.class);

        sourceValidationService = new XmlValidationService(DefaultAtlasConversionService.getInstance());
        sourceValidationService.setMode(AtlasModuleMode.SOURCE);
        targetValidationService = new XmlValidationService(DefaultAtlasConversionService.getInstance());
        targetValidationService.setMode(AtlasModuleMode.TARGET);
        validationHelper = new AtlasValidationTestHelper();
        validations = validationHelper.getValidation();
    }

    @After
    public void tearDown() {
        xmlModelFactory = null;
        mappingUtil = null;
        sourceValidationService = null;
        targetValidationService = null;
        validationHelper = null;
        validations = null;
    }

    protected AtlasMapping getAtlasMappingFullValid() throws Exception {
        AtlasMapping mapping = AtlasModelFactory.createAtlasMapping();

        mapping.setName("thisis_a_valid.name");

        mapping.getDataSource().add(generateDataSource("atlas:xml:MockXml", DataSourceType.SOURCE));
        mapping.getDataSource().add(generateDataSource("atlas:xml:MockXml", DataSourceType.TARGET));

        Mapping mapMapping = AtlasModelFactory.createMapping(MappingType.MAP);
        Mapping sepMapping = AtlasModelFactory.createMapping(MappingType.SEPARATE);

        // MappedField
        XmlField inputField = AtlasXmlModelFactory.createXmlField();
        inputField.setFieldType(FieldType.STRING);
        inputField.setPath("firstName");

        XmlField outputField = AtlasXmlModelFactory.createXmlField();
        outputField.setFieldType(FieldType.STRING);
        outputField.setPath("firstName");

        mapMapping.getInputField().add(inputField);
        mapMapping.getOutputField().add(outputField);

        XmlField sIJavaField = AtlasXmlModelFactory.createXmlField();
        sIJavaField.setFieldType(FieldType.STRING);
        sIJavaField.setPath("displayName");
        sepMapping.getInputField().add(sIJavaField);

        XmlField sOJavaField = AtlasXmlModelFactory.createXmlField();
        sOJavaField.setFieldType(FieldType.STRING);
        sOJavaField.setPath("lastName");
        sOJavaField.setIndex(1);
        sepMapping.getOutputField().add(sOJavaField);

        mapping.getMappings().getMapping().add(mapMapping);
        mapping.getMappings().getMapping().add(sepMapping);
        return mapping;
    }

    protected DataSource generateDataSource(String uri, DataSourceType type) {
        DataSource ds = new DataSource();
        ds.setUri(uri);
        ds.setDataSourceType(type);
        return ds;
    }

    protected Mapping createMockMapping() {
        // Mock MappedField
        MockField inputField = new MockField();
        inputField.setName("input.name");
        MockField outputField = new MockField();
        outputField.setName("out.name");

        Mapping mapping = new Mapping();
        mapping.setMappingType(MappingType.MAP);
        mapping.getInputField().add(inputField);
        mapping.getOutputField().add(inputField);
        return mapping;
    }

    protected void debugErrors(List<Validation> validations) {
        for (Validation validation : validations) {
            logger.debug(AtlasValidationTestHelper.validationToString(validation));
        }
    }

    // @Test
    // @Ignore // Note: manual utility to assist in creating files
    // public void saveSampleFile() throws Exception {
    // AtlasMappingUtil util = new
    // AtlasMappingUtil("io.atlasmap.v2:io.atlasmap.java.v2");
    //
    // AtlasMapping mapping = getAtlasMappingFullValid();
    // util.marshallMapping(mapping,
    // "src/test/resources/mappings/HappyPathMapping.xml");
    //
    // mapping = getAtlasMappingFullValid();
    // mapping.getMappings().getMapping().clear();
    // mapping.getMappings().getMapping().add(createMockMapping());
    // mappingUtil.marshallMapping(mapping,
    // "src/test/resources/mappings/MisMatchedFieldTypes.xml");
    //
    // }

    @Test
    public void testValidateMappingHappyPath() throws Exception {
        AtlasMapping mapping = getAtlasMappingFullValid();
        assertNotNull(mapping);
        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));
        assertFalse(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
    }

    @Test
    public void testValidateMappingHappyPathFromFile() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        assertFalse(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
    }

    @Test
    public void testValidateMappingMismatchedFieldType() throws Exception {
        AtlasMapping mapping = getAtlasMappingFullValid();
        assertNotNull(mapping);
        mapping.getMappings().getMapping().clear();

        // Mock MappedField
        mapping.getMappings().getMapping().add(createMockMapping());

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        assertTrue(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
    }

    @Test
    public void testValidateMappingInvalidModuleType() throws Exception {
        AtlasMapping mapping = AtlasModelFactory.createAtlasMapping();

        mapping.setName("thisis_a_valid.name");
        mapping.getDataSource().add(generateDataSource("atlas:java", DataSourceType.SOURCE));
        mapping.getDataSource().add(generateDataSource("atlas:json", DataSourceType.TARGET));

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        assertTrue(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
    }

    @Test
    public void testValidateMappingInvalidSeparateInputFieldType() throws Exception {
        AtlasMapping atlasMapping = getAtlasMappingFullValid();

        Mapping separateFieldMapping = AtlasModelFactory.createMapping(MappingType.SEPARATE);

        XmlField bIJavaField = xmlModelFactory.createXmlField();
        bIJavaField.setFieldType(FieldType.BOOLEAN);
        bIJavaField.setValue(Boolean.TRUE);
        bIJavaField.setPath("firstName");

        separateFieldMapping.getInputField().add(bIJavaField);

        XmlField sOJavaField = xmlModelFactory.createXmlField();
        sOJavaField.setFieldType(FieldType.STRING);
        sOJavaField.setPath("lastName");
        sOJavaField.setIndex(0);
        separateFieldMapping.getOutputField().add(sOJavaField);

        atlasMapping.getMappings().getMapping().add(separateFieldMapping);

        validations.addAll(sourceValidationService.validateMapping(atlasMapping));
        validations.addAll(targetValidationService.validateMapping(atlasMapping));

        assertTrue(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertTrue(validationHelper.hasInfos());

        assertEquals(new Integer(2), new Integer(validationHelper.getCount()));

        Validation validation = validations.get(0);
        assertNotNull(validation);
        assertEquals("Input.Field", validation.getField());
        assertEquals("(BOOLEAN)", validation.getValue().toString());
        assertEquals("Input field must be of type STRING for a Separate Mapping", validation.getMessage());
        assertEquals(ValidationStatus.ERROR, validation.getStatus());
        validation = validations.get(1);
        assertNotNull(validation);
        assertEquals("Field.Input/Output.conversion", validation.getField());
        assertEquals("(BOOLEAN) --> (STRING)", validation.getValue().toString());
        assertEquals("Conversion between source and target types is supported", validation.getMessage());
        assertEquals(ValidationStatus.INFO, validation.getStatus());
    }

    @Test
    public void testValidateMappingSupportedSourceToTargetConversion() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        Mapping fieldMapping = (Mapping) mapping.getMappings().getMapping().get(0);

        XmlField in = (XmlField) fieldMapping.getInputField().get(0);
        in.setFieldType(FieldType.CHAR);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        if (logger.isDebugEnabled()) {
            debugErrors(validations);
        }
        assertFalse(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertTrue(validationHelper.hasInfos());
    }

    @Test
    public void testValidateAtlasMappingFileConversionRequired() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        Mapping fieldMapping = (Mapping) mapping.getMappings().getMapping().get(0);

        XmlComplexType complex = xmlModelFactory.createXmlComplexType();
        complex.setFieldType(FieldType.COMPLEX);
        XmlField in = (XmlField) fieldMapping.getInputField().get(0);
        complex.setXmlFields(new XmlFields());
        complex.setPath("/nest");
        complex.setName("nest");
        in.setPath("/nest/" + in.getPath());
        complex.getXmlFields().getXmlField().add(in);
        fieldMapping.getInputField().set(0, complex);

        XmlField out = (XmlField) fieldMapping.getOutputField().get(0);
        out.setFieldType(FieldType.STRING);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        assertFalse(validationHelper.hasErrors());
        assertTrue(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
        assertThat(1, is(validationHelper.getCount()));
        assertTrue(validations.stream().anyMatch(me -> me.getField().equals("Field.Input/Output.conversion")));

        Long errorCount = validations.stream()
                .filter(atlasMappingError -> atlasMappingError.getStatus().compareTo(ValidationStatus.WARN) == 0)
                .count();
        assertNotNull(errorCount);
        assertEquals(1L, errorCount.longValue());
    }

    @Test
    public void testValidateMappingSourceToTargetRangeConcerns() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        Mapping fieldMapping = (Mapping) mapping.getMappings().getMapping().get(0);

        XmlField in = (XmlField) fieldMapping.getInputField().get(0);
        in.setFieldType(FieldType.DOUBLE);

        XmlField out = (XmlField) fieldMapping.getOutputField().get(0);
        out.setFieldType(FieldType.LONG);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        if (logger.isDebugEnabled()) {
            debugErrors(validations);
        }
        assertFalse(validationHelper.hasErrors());
        assertTrue(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
        assertThat(1, is(validationHelper.getCount()));

        assertTrue(
                validations.stream().anyMatch(atlasMappingError -> atlasMappingError.getMessage().contains("range")));
    }

    @Test
    public void testValidateMappingSourceToTargetFormatConcerns() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        Mapping fieldMapping = (Mapping) mapping.getMappings().getMapping().get(0);

        XmlField in = (XmlField) fieldMapping.getInputField().get(0);
        in.setFieldType(FieldType.STRING);

        XmlField out = (XmlField) fieldMapping.getOutputField().get(0);
        out.setFieldType(FieldType.LONG);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        if (logger.isDebugEnabled()) {
            debugErrors(validations);
        }
        assertFalse(validationHelper.hasErrors());
        assertTrue(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
        assertThat(2, is(validationHelper.getCount()));

        assertTrue(
                validations.stream().anyMatch(atlasMappingError -> atlasMappingError.getMessage().contains("range")));
        assertTrue(
                validations.stream().anyMatch(atlasMappingError -> atlasMappingError.getMessage().contains("format")));
    }

    @Test
    public void testValidateMappingPathNull() throws Exception {
        AtlasMapping mapping = mappingUtil.loadMapping("src/test/resources/mappings/HappyPathMapping.xml");
        assertNotNull(mapping);

        Mapping fieldMapping = (Mapping) mapping.getMappings().getMapping().get(0);

        XmlField in = (XmlField) fieldMapping.getInputField().get(0);
        in.setPath(null);

        validations.addAll(sourceValidationService.validateMapping(mapping));
        validations.addAll(targetValidationService.validateMapping(mapping));

        assertTrue(validationHelper.hasErrors());
        assertFalse(validationHelper.hasWarnings());
        assertFalse(validationHelper.hasInfos());
    }

    public static <T> Collector<T, ?, T> singletonCollector() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() != 1) {
                throw new IllegalStateException();
            }
            return list.get(0);
        });
    }
}
