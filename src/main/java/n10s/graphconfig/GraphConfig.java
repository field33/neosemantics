package n10s.graphconfig;

import n10s.result.GraphConfigItemResult;
import org.apache.commons.collections.map.HashedMap;
import org.eclipse.rdf4j.model.util.URIUtil;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static n10s.graphconfig.Params.DEFAULT_BASE_SCH_NS;
import static n10s.graphconfig.Params.DEFAULT_BASE_SCH_PREFIX;
import static n10s.graphconfig.Params.PREFIX_PATTERN;

public class GraphConfig {

  public static final int GRAPHCONF_MODE_LPG = 1;
  public static final int GRAPHCONF_MODE_RDF = 2;

  public static final int GRAPHCONF_VOC_URI_SHORTEN = 0;
  public static final int GRAPHCONF_VOC_URI_SHORTEN_STRICT = 1;
  public static final int GRAPHCONF_VOC_URI_IGNORE = 2;
  public static final int GRAPHCONF_VOC_URI_MAP = 3;
  public static final int GRAPHCONF_VOC_URI_KEEP = 4;

  public static final String GRAPHCONF_VOC_URI_SHORTEN_STR = "SHORTEN";
  public static final String GRAPHCONF_VOC_URI_SHORTEN_STRICT_STR = "SHORTEN_STRICT";
  public static final String GRAPHCONF_VOC_URI_IGNORE_STR = "IGNORE";
  public static final String GRAPHCONF_VOC_URI_MAP_STR = "MAP";
  public static final String GRAPHCONF_VOC_URI_KEEP_STR = "KEEP";

  public static final int GRAPHCONF_MULTIVAL_PROP_OVERWRITE = 0;
  public static final int GRAPHCONF_MULTIVAL_PROP_ARRAY = 1;
  //static final int GRAPHCONF_MULTIVAL_PROP_REIFY = 2; //not in use

  public static final String GRAPHCONF_MULTIVAL_PROP_OVERWRITE_STR = "OVERWRITE";
  public static final String GRAPHCONF_MULTIVAL_PROP_ARRAY_STR = "ARRAY";

  public static final int GRAPHCONF_RDFTYPES_AS_LABELS = 0;
  public static final int GRAPHCONF_RDFTYPES_AS_NODES = 1;
  public static final int GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES = 2;

  public static final String GRAPHCONF_RDFTYPES_AS_LABELS_STR = "LABELS";
  public static final String GRAPHCONF_RDFTYPES_AS_NODES_STR = "NODES";
  public static final String GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES_STR = "LABELS_AND_NODES";

  private static final String DEFAULT_CLASS_LABEL_NAME = "Class";
  private static final String DEFAULT_SCO_REL_NAME = "SCO";
  private static final String DEFAULT_DATATYPEPROP_LABEL_NAME = "Property";
  private static final String DEFAULT_OBJECTPROP_LABEL_NAME = "Relationship";
  private static final String DEFAULT_SPO_REL_NAME = "SPO";
  private static final String DEFAULT_DOMAIN_REL_NAME = "DOMAIN";
  private static final String DEFAULT_RANGE_REL_NAME = "RANGE";

  private int handleVocabUris;
  private int handleMultival;
  private int handleRDFTypes;
  private boolean keepLangTag;
  private boolean applyNeo4jNaming;
  private boolean keepCustomDataTypes;
  private Set<String> multivalPropList;
  private Set<String> customDataTypePropList;
  private String classLabelName;
  private String subClassOfRelName;
  private String dataTypePropertyLabelName;
  private String objectPropertyLabelName;
  private String subPropertyOfRelName;
  private String domainRelName;
  private String rangeRelName;
  private String baseSchemaNamespace;
  private String baseSchemaNamespacePrefix;

  private Map<String, Object> forciblyAssignedOnImportNodeProperties;


  public GraphConfig(Map<String, Object> props) throws InvalidParamException {

    this.handleVocabUris = (props.containsKey("handleVocabUris") ? parseHandleVocabUrisValue(
        (String) props.get("handleVocabUris")) : GRAPHCONF_VOC_URI_SHORTEN);
    this.handleMultival = (props.containsKey("handleMultival") ? parseHandleMultivalValue(
        (String) props.get("handleMultival")) : GRAPHCONF_MULTIVAL_PROP_OVERWRITE);

    this.handleRDFTypes = (props.containsKey("handleRDFTypes") ? parseHandleRDFTypesValue(
        (String) props
            .get("handleRDFTypes")) : GRAPHCONF_RDFTYPES_AS_LABELS);
    this.keepLangTag = (props.containsKey("keepLangTag") && (boolean) props
        .get("keepLangTag"));
    this.applyNeo4jNaming = (props.containsKey("applyNeo4jNaming") && (boolean) props
        .get("applyNeo4jNaming"));
    this.keepCustomDataTypes = (props.containsKey("keepCustomDataTypes") && (boolean) props
        .get("keepCustomDataTypes"));
    this.multivalPropList = (props.containsKey("multivalPropList")
        ? (props.get("multivalPropList") != null ? ((List<String>) props.get("multivalPropList"))
        .stream().collect(Collectors.toSet()) : null)
        : null);
    this.customDataTypePropList = (props.containsKey("customDataTypePropList")
        ? (props.get("customDataTypePropList") != null ? ((List<String>) props
        .get("customDataTypePropList"))
        .stream().collect(Collectors.toSet()) : null)
        : null);
    this.baseSchemaNamespace = (props.containsKey("baseSchemaNamespace") && URIUtil.isCorrectURISplit(
            (String) props.get("baseSchemaNamespace"),"someLocalName")?
            (String)props.get("baseSchemaNamespace"): null);

    if (props.containsKey("baseSchemaPrefix")){
      Matcher matcher = PREFIX_PATTERN.matcher((String)props.get("baseSchemaPrefix"));
      if(matcher.matches()) {
        //should we check that it's not one of the default ones? No because this is for the 'IGNORE' and native PG case
        this.baseSchemaNamespacePrefix = (String)props.get("baseSchemaPrefix");
      } else{
        //not a valid prefix so use default
        this.baseSchemaNamespacePrefix = null;
      }
    } else {
      //no def in the config, use default
      this.baseSchemaNamespacePrefix = null;
    }

    // Ontology config

    this.classLabelName = props.containsKey("classLabel") ? (String) props.get("classLabel")
        : DEFAULT_CLASS_LABEL_NAME;
    this.subClassOfRelName =
        props.containsKey("subClassOfRel") ? (String) props.get("subClassOfRel")
            : DEFAULT_SCO_REL_NAME;
    this.dataTypePropertyLabelName =
        props.containsKey("dataTypePropertyLabel") ? (String) props.get("dataTypePropertyLabel")
            : DEFAULT_DATATYPEPROP_LABEL_NAME;
    this.objectPropertyLabelName =
        props.containsKey("objectPropertyLabel") ? (String) props.get("objectPropertyLabel")
            : DEFAULT_OBJECTPROP_LABEL_NAME;
    this.subPropertyOfRelName =
        props.containsKey("subPropertyOfRel") ? (String) props.get("subPropertyOfRel")
            : DEFAULT_SPO_REL_NAME;
    this.domainRelName =
        props.containsKey("domainRel") ? (String) props.get("domainRel") : DEFAULT_DOMAIN_REL_NAME;
    this.rangeRelName =
        props.containsKey("rangeRel") ? (String) props.get("rangeRel") : DEFAULT_RANGE_REL_NAME;

    this.forciblyAssignedOnImportNodeProperties = props.containsKey("forciblyAssignedOnImportNodeProperties") && props.get("forciblyAssignedOnImportNodeProperties") != null
        ? (Map<String, Object>) props.get("forciblyAssignedOnImportNodeProperties")
        : new HashMap();
  }

  public GraphConfig(Transaction tx) throws GraphConfigNotFound {
    Result gcResult = tx.execute("MATCH (gc:_GraphConfig) OPTIONAL MATCH (gc)-[:HAS*0..1]-(nodeProps:_ForciblyAssignedOnImportNodeProperties) RETURN gc, nodeProps");
    if (gcResult.hasNext()) {
      Map<String, Object> singleRecord = gcResult.next();
      Map<String, Object> graphConfigProperties = ((Node) singleRecord.get("gc"))
          .getAllProperties();
      this.handleVocabUris = (int) graphConfigProperties.get("_handleVocabUris");
      this.handleMultival = (int) graphConfigProperties.get("_handleMultival");
      this.handleRDFTypes = (int) graphConfigProperties.get("_handleRDFTypes");
      this.keepLangTag = (boolean) graphConfigProperties.get("_keepLangTag");
      this.keepCustomDataTypes = (boolean) graphConfigProperties.get("_keepCustomDataTypes");
      this.applyNeo4jNaming = (boolean) graphConfigProperties.get("_applyNeo4jNaming");
      this.multivalPropList = getListOfStringsOrNull(graphConfigProperties, "_multivalPropList");
      this.customDataTypePropList = getListOfStringsOrNull(graphConfigProperties,
          "_customDataTypePropList");
      this.baseSchemaNamespace = (String)graphConfigProperties.get("_baseSchemaNamespace");
      this.baseSchemaNamespacePrefix = (String)graphConfigProperties.get("_baseSchemaPrefix");

      this.classLabelName =
          graphConfigProperties.containsKey("_classLabel") ? (String) graphConfigProperties
              .get("_classLabel")
              : DEFAULT_CLASS_LABEL_NAME;
      this.subClassOfRelName =
          graphConfigProperties.containsKey("_subClassOfRel") ? (String) graphConfigProperties
              .get("_subClassOfRel")
              : DEFAULT_SCO_REL_NAME;
      this.dataTypePropertyLabelName =
          graphConfigProperties.containsKey("_dataTypePropertyLabel")
              ? (String) graphConfigProperties.get("_dataTypePropertyLabel")
              : DEFAULT_DATATYPEPROP_LABEL_NAME;
      this.objectPropertyLabelName =
          graphConfigProperties.containsKey("_objectPropertyLabel") ? (String) graphConfigProperties
              .get("_objectPropertyLabel")
              : DEFAULT_OBJECTPROP_LABEL_NAME;
      this.subPropertyOfRelName =
          graphConfigProperties.containsKey("_subPropertyOfRel") ? (String) graphConfigProperties
              .get("_subPropertyOfRel")
              : DEFAULT_SPO_REL_NAME;
      this.domainRelName =
          graphConfigProperties.containsKey("_domainRel") ? (String) graphConfigProperties
              .get("_domainRel") : DEFAULT_DOMAIN_REL_NAME;
      this.rangeRelName =
          graphConfigProperties.containsKey("_rangeRel") ? (String) graphConfigProperties
              .get("_rangeRel") : DEFAULT_RANGE_REL_NAME;

      if (singleRecord.containsKey("nodeProps") && singleRecord.get("nodeProps") != null) {
        Map<String, Object> nodeProperties = ((Node) singleRecord.get("nodeProps"))
            .getAllProperties();
        nodeProperties.remove("identity");
        this.forciblyAssignedOnImportNodeProperties = nodeProperties;
      } else {
        this.forciblyAssignedOnImportNodeProperties = new HashMap();
      }
    } else {
      throw new GraphConfigNotFound();
    }
  }

  private Set<String> getListOfStringsOrNull(Map<String, Object> gcp, String key) {
    if (gcp.containsKey(key)) {
      Set<String> resultSet = new HashSet<>();
      String[] arrayOfStrings = (String[]) gcp.get(key);
      for (String str : arrayOfStrings) {
        resultSet.add(str);
      }
      return resultSet;
    } else {
      return null;
    }
  }


  public int getGraphMode() {
    if (handleVocabUris == GRAPHCONF_VOC_URI_SHORTEN ||
        handleVocabUris == GRAPHCONF_VOC_URI_SHORTEN_STRICT ||
        handleVocabUris == GRAPHCONF_VOC_URI_KEEP) {
      return GRAPHCONF_MODE_RDF;
    } else if (handleVocabUris == GRAPHCONF_VOC_URI_IGNORE ||
            handleVocabUris == GRAPHCONF_VOC_URI_MAP) {
      return GRAPHCONF_MODE_LPG;
    } else {
      //Default to LPG?
      return GRAPHCONF_MODE_LPG;
    }
  }

  public int parseHandleVocabUrisValue(String handleVocUrisAsText) throws InvalidParamException {
    if (handleVocUrisAsText.equals(GRAPHCONF_VOC_URI_SHORTEN_STR)) {
      return GRAPHCONF_VOC_URI_SHORTEN;
    } else if (handleVocUrisAsText.equals(GRAPHCONF_VOC_URI_SHORTEN_STRICT_STR)) {
      return GRAPHCONF_VOC_URI_SHORTEN_STRICT;
    } else if (handleVocUrisAsText.equals(GRAPHCONF_VOC_URI_IGNORE_STR)) {
      return GRAPHCONF_VOC_URI_IGNORE;
    } else if (handleVocUrisAsText.equals(GRAPHCONF_VOC_URI_MAP_STR)) {
      return GRAPHCONF_VOC_URI_MAP;
    } else if (handleVocUrisAsText.equals(GRAPHCONF_VOC_URI_KEEP_STR)) {
      return GRAPHCONF_VOC_URI_KEEP;
    } else {
      throw new InvalidParamException(
          handleVocUrisAsText + " is not a valid option for param 'handleVocabUris'");
    }
  }

  public int parseHandleMultivalValue(String multivalAsText) throws InvalidParamException {
    if (multivalAsText.equals(GRAPHCONF_MULTIVAL_PROP_OVERWRITE_STR)) {
      return GRAPHCONF_MULTIVAL_PROP_OVERWRITE;
    } else if (multivalAsText.equals(GRAPHCONF_MULTIVAL_PROP_ARRAY_STR)) {
      return GRAPHCONF_MULTIVAL_PROP_ARRAY;
    } else {
      throw new InvalidParamException(
          multivalAsText + " is not a valid option for param 'handleMultival'");
    }
  }


  private int parseHandleRDFTypesValue(String handleRDFTypesAsText) throws InvalidParamException {
    if (handleRDFTypesAsText.equals(GRAPHCONF_RDFTYPES_AS_LABELS_STR)) {
      return GRAPHCONF_RDFTYPES_AS_LABELS;
    } else if (handleRDFTypesAsText.equals(GRAPHCONF_RDFTYPES_AS_NODES_STR)) {
      return GRAPHCONF_RDFTYPES_AS_NODES;
    } else if (handleRDFTypesAsText.equals(GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES_STR)) {
      return GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES;
    } else {
      throw new InvalidParamException(
          handleRDFTypesAsText + " is not a valid option for param 'handleRDFTypes'");
    }
  }

  public String getHandleVocabUrisAsString() {
    switch (this.handleVocabUris) {
      case GRAPHCONF_VOC_URI_SHORTEN:
        return GRAPHCONF_VOC_URI_SHORTEN_STR;
      case GRAPHCONF_VOC_URI_SHORTEN_STRICT:
        return GRAPHCONF_VOC_URI_SHORTEN_STRICT_STR;
      case GRAPHCONF_VOC_URI_IGNORE:
        return GRAPHCONF_VOC_URI_IGNORE_STR;
      case GRAPHCONF_VOC_URI_MAP:
        return GRAPHCONF_VOC_URI_MAP_STR;
      default:
        return GRAPHCONF_VOC_URI_KEEP_STR;
    }
  }

  public String getHandleMultivalAsString() {
    switch (this.handleMultival) {
      case GRAPHCONF_MULTIVAL_PROP_OVERWRITE:
        return GRAPHCONF_MULTIVAL_PROP_OVERWRITE_STR;
      case GRAPHCONF_MULTIVAL_PROP_ARRAY:
        return GRAPHCONF_MULTIVAL_PROP_ARRAY_STR;
      default:
        return GRAPHCONF_MULTIVAL_PROP_OVERWRITE_STR;
    }
  }

  public String getHandleRDFTypesAsString() {
    switch (this.handleRDFTypes) {
      case GRAPHCONF_RDFTYPES_AS_LABELS:
        return GRAPHCONF_RDFTYPES_AS_LABELS_STR;
      case GRAPHCONF_RDFTYPES_AS_NODES:
        return GRAPHCONF_RDFTYPES_AS_NODES_STR;
      case GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES:
        return GRAPHCONF_RDFTYPES_AS_LABELS_AND_NODES_STR;
      default:
        return GRAPHCONF_RDFTYPES_AS_LABELS_STR;
    }
  }

  public List<GraphConfigItemResult> getAsGraphConfigResults() {

    List<GraphConfigItemResult> result = new ArrayList<>();
    result.add(new GraphConfigItemResult("handleVocabUris", getHandleVocabUrisAsString()));
    result.add(new GraphConfigItemResult("handleMultival", getHandleMultivalAsString()));
    result.add(new GraphConfigItemResult("handleRDFTypes", getHandleRDFTypesAsString()));
    result.add(new GraphConfigItemResult("keepLangTag", isKeepLangTag()));
    if(getMultivalPropList()!=null) {
      result.add(new GraphConfigItemResult("multivalPropList", getMultivalPropList()));
    }
    result.add(new GraphConfigItemResult("keepCustomDataTypes", isKeepCustomDataTypes()));
    if (getCustomDataTypePropList()!=null){
      result.add(new GraphConfigItemResult("customDataTypePropList", getCustomDataTypePropList()));
    }
    result.add(new GraphConfigItemResult("applyNeo4jNaming", isApplyNeo4jNaming()));
    if (getBaseSchemaNamespace()!=null){
      result.add(new GraphConfigItemResult("baseSchemaNamespace", getBaseSchemaNamespace()));
    }
    if (getBaseSchemaNamespacePrefix()!=null){
      result.add(new GraphConfigItemResult("baseSchemaPrefix", getBaseSchemaNamespacePrefix()));
    }
    //onto  import config
    result.add(new GraphConfigItemResult("classLabel", getClassLabelName()));
    result.add(new GraphConfigItemResult("subClassOfRel", getSubClassOfRelName()));
    result.add(new GraphConfigItemResult("dataTypePropertyLabel", getDataTypePropertyLabelName()));
    result.add(new GraphConfigItemResult("objectPropertyLabel", getObjectPropertyLabelName()));
    result.add(new GraphConfigItemResult("subPropertyOfRel", getSubPropertyOfRelName()));
    result.add(new GraphConfigItemResult("domainRel", getDomainRelName()));
    result.add(new GraphConfigItemResult("rangeRel", getRangeRelName()));

    result.add(new GraphConfigItemResult("forciblyAssignedOnImportNodeProperties", getForciblyAssignedOnImportNodeProperties()));

    return result;
  }

  public Map<String, Object> serialiseConfig() {
    Map<String, Object> configAsMap = new HashMap<>();

    //ONLY ADD IF NOT DEFAULT
    // if (this.handleVocabUris != 0) {
    configAsMap.put("_handleVocabUris", this.handleVocabUris);
    configAsMap.put("_handleMultival", this.handleMultival);
    configAsMap.put("_handleRDFTypes", this.handleRDFTypes);
    configAsMap.put("_keepLangTag", this.keepLangTag);
    configAsMap.put("_keepCustomDataTypes", this.keepCustomDataTypes);
    configAsMap.put("_applyNeo4jNaming", this.applyNeo4jNaming);
    configAsMap.put("_multivalPropList", this.multivalPropList);
    configAsMap.put("_customDataTypePropList", this.customDataTypePropList);
    configAsMap.put("_baseSchemaNamespace", this.baseSchemaNamespace);
    configAsMap.put("_baseSchemaPrefix", this.baseSchemaNamespacePrefix);
    configAsMap.put("_classLabel", this.classLabelName);
    configAsMap.put("_subClassOfRel", this.subClassOfRelName);
    configAsMap.put("_dataTypePropertyLabel", this.dataTypePropertyLabelName);
    configAsMap.put("_objectPropertyLabel", this.objectPropertyLabelName);
    configAsMap.put("_subPropertyOfRel", this.subPropertyOfRelName);
    configAsMap.put("_domainRel", this.domainRelName);
    configAsMap.put("_rangeRel", this.rangeRelName);

    configAsMap.put("_forciblyAssignedOnImportNodeProperties", this.forciblyAssignedOnImportNodeProperties);

    return configAsMap;
  }

  public int getHandleVocabUris() {
    return handleVocabUris;
  }

  public int getHandleMultival() {
    return handleMultival;
  }

  public int getHandleRDFTypes() {
    return handleRDFTypes;
  }

  public boolean isKeepLangTag() {
    return keepLangTag;
  }

  public boolean isApplyNeo4jNaming() {
    return applyNeo4jNaming;
  }

  public boolean isKeepCustomDataTypes() {
    return keepCustomDataTypes;
  }

  public Set<String> getMultivalPropList() {
    return multivalPropList;
  }

  public Set<String> getCustomDataTypePropList() {
    return customDataTypePropList;
  }

  public String getBaseSchemaNamespace() {
    if (baseSchemaNamespace != null) {
      return baseSchemaNamespace;
    } else {
      return DEFAULT_BASE_SCH_NS;
    }
  }

  public String getBaseSchemaNamespacePrefix() {

    if(baseSchemaNamespacePrefix!= null) {
      return baseSchemaNamespacePrefix;
    } else {
      return DEFAULT_BASE_SCH_PREFIX;
    }
  }

  public String getClassLabelName() {
    return classLabelName;
  }

  public String getObjectPropertyLabelName() {
    return objectPropertyLabelName;
  }

  public String getDataTypePropertyLabelName() {
    return dataTypePropertyLabelName;
  }

  public String getSubClassOfRelName() {
    return subClassOfRelName;
  }

  public String getSubPropertyOfRelName() {
    return subPropertyOfRelName;
  }

  public String getDomainRelName() {
    return domainRelName;
  }

  public String getRangeRelName() {
    return rangeRelName;
  }

  public String getRelatedConceptRelName() {
    return "RELATED";
    //TODO: create a config param for skosRelatedConceptRelName
  }

  public Map<String, Object> getForciblyAssignedOnImportNodeProperties() {
    if (this.forciblyAssignedOnImportNodeProperties == null) {
      this.forciblyAssignedOnImportNodeProperties = new HashMap();
    }

    return this.forciblyAssignedOnImportNodeProperties;
  }

  public void add(Map<String, Object> props) throws InvalidParamException {
    if (props.containsKey("handleVocabUris")) {
      this.handleVocabUris = parseHandleVocabUrisValue((String) props.get("handleVocabUris"));
    }
    if (props.containsKey("handleMultival")) {
      this.handleMultival = parseHandleMultivalValue((String) props.get("handleMultival"));
    }
    if (props.containsKey("handleRDFTypes")) {
      this.handleRDFTypes = parseHandleRDFTypesValue((String) props.get("handleRDFTypes"));
    }
    if ((props.containsKey("keepLangTag") && (boolean) props.get("keepLangTag"))) {
      this.keepLangTag = (boolean) props.get("keepLangTag");
    }
    if (props.containsKey("applyNeo4jNaming") && (boolean) props.get("applyNeo4jNaming")) {
      this.applyNeo4jNaming = (boolean) props.get("applyNeo4jNaming");
    }
    if ((props.containsKey("keepCustomDataTypes") && (boolean) props.get("keepCustomDataTypes"))) {
      this.keepCustomDataTypes = (boolean) props.get("keepCustomDataTypes");
    }
    if (props.containsKey("multivalPropList")) {
      this.multivalPropList = (props.get("multivalPropList") != null ?
          ((List<String>) props.get("multivalPropList")).stream().collect(Collectors.toSet())
          : null);
    }
    if (props.containsKey("customDataTypePropList")) {
      this.customDataTypePropList = (props.get("customDataTypePropList") != null ?
          ((List<String>) props.get("customDataTypePropList")).stream().collect(Collectors.toSet())
          : null);
    }
    if (props.containsKey("baseSchemaNamespace") && URIUtil.isCorrectURISplit(
            (String) props.get("baseSchemaNamespace"),"someLocalName")) {
      this.baseSchemaNamespace =  (String)props.get("baseSchemaNamespace");
    }


    if (props.containsKey("classLabel")) {
      this.classLabelName = (String) props.get("classLabel");
    }
    if (props.containsKey("subClassOfRel")) {
      this.subClassOfRelName = (String) props.get("subClassOfRel");
    }
    if (props.containsKey("dataTypePropertyLabel")) {
      this.dataTypePropertyLabelName = (String) props.get("dataTypePropertyLabel");
    }
    if (props.containsKey("objectPropertyLabel")) {
      this.objectPropertyLabelName = (String) props.get("objectPropertyLabel");
    }
    if (props.containsKey("subPropertyOfRel")) {
      this.subPropertyOfRelName = (String) props.get("subPropertyOfRel");
    }
    if (props.containsKey("domainRel")) {
      this.domainRelName = (String) props.get("domainRel");
    }
    if (props.containsKey("rangeRel")) {
      this.rangeRelName = (String) props.get("rangeRel");
    }
    if (props.containsKey("forciblyAssignedOnImportNodeProperties")) {
      if (props.containsKey("forciblyAssignedOnImportNodeProperties") && props.get("forciblyAssignedOnImportNodeProperties") != null) {
        this.forciblyAssignedOnImportNodeProperties = (Map<String, Object>) props.get("forciblyAssignedOnImportNodeProperties");
      } else {
        this.forciblyAssignedOnImportNodeProperties = new HashedMap();
      }
    }
  }

  public class InvalidParamException extends Throwable {

    public InvalidParamException(String msg) {
      super(msg);
    }
  }

  public class GraphConfigNotFound extends Throwable {

  }
}
