/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.config;

import org.apache.openejb.OpenEJBException;
import org.apache.openejb.util.Strings;
import org.apache.openejb.util.Logger;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.core.cmp.jpa.JpaCmpEngine;
import org.apache.openejb.core.cmp.CmpUtil;
import org.apache.openejb.jee.CmpField;
import org.apache.openejb.jee.CmpVersion;
import org.apache.openejb.jee.EjbJar;
import org.apache.openejb.jee.EjbRelation;
import org.apache.openejb.jee.EjbRelationshipRole;
import org.apache.openejb.jee.EntityBean;
import org.apache.openejb.jee.Multiplicity;
import org.apache.openejb.jee.PersistenceContextRef;
import org.apache.openejb.jee.PersistenceType;
import org.apache.openejb.jee.RelationshipRoleSource;
import org.apache.openejb.jee.Relationships;
import org.apache.openejb.jee.Query;
import org.apache.openejb.jee.QueryMethod;
import org.apache.openejb.jee.EnterpriseBean;
import org.apache.openejb.jee.oejb3.OpenejbJar;
import org.apache.openejb.jee.oejb3.EjbDeployment;
import org.apache.openejb.jee.jpa.Basic;
import org.apache.openejb.jee.jpa.CascadeType;
import org.apache.openejb.jee.jpa.Entity;
import org.apache.openejb.jee.jpa.EntityMappings;
import org.apache.openejb.jee.jpa.Id;
import org.apache.openejb.jee.jpa.ManyToMany;
import org.apache.openejb.jee.jpa.ManyToOne;
import org.apache.openejb.jee.jpa.OneToMany;
import org.apache.openejb.jee.jpa.OneToOne;
import org.apache.openejb.jee.jpa.RelationField;
import org.apache.openejb.jee.jpa.Transient;
import org.apache.openejb.jee.jpa.MappedSuperclass;
import org.apache.openejb.jee.jpa.Mapping;
import org.apache.openejb.jee.jpa.AttributeOverride;
import org.apache.openejb.jee.jpa.Field;
import org.apache.openejb.jee.jpa.NamedQuery;
import org.apache.openejb.jee.jpa.IdClass;
import org.apache.openejb.jee.jpa.GeneratedValue;
import org.apache.openejb.jee.jpa.GenerationType;
import org.apache.openejb.jee.jpa.Attributes;
import org.apache.openejb.jee.jpa.unit.Persistence;
import org.apache.openejb.jee.jpa.unit.PersistenceUnit;
import org.apache.openejb.jee.jpa.unit.TransactionType;
import org.apache.openejb.jee.jpa.unit.Properties;

import javax.ejb.EJBLocalObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.TreeMap;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

public class CmpJpaConversion implements DynamicDeployer {

    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB_STARTUP_CONFIG, CmpJpaConversion.class);

    private static final String CMP_PERSISTENCE_UNIT_NAME = "cmp";

    private static final Set<String> ENHANCEED_FIELDS = Collections.unmodifiableSet(new TreeSet<String>(Arrays.asList(
            "pcInheritedFieldCount",
            "pcFieldNames",
            "pcFieldTypes",
            "pcFieldFlags",
            "pcPCSuperclass",
            "pcStateManager",
            "class$Ljava$lang$String",
            "class$Ljava$lang$Integer",
            "class$Lcom$sun$ts$tests$common$ejb$wrappers$CMP11Wrapper",
            "pcDetachedState",
            "serialVersionUID"
    )));

    public AppModule deploy(AppModule appModule) throws OpenEJBException {

        if (!hasCmpEntities(appModule)) return appModule;

        // todo scan existing persistence module for all entity mappings and don't generate mappings for them

        // create mappings if no mappings currently exist 
        EntityMappings cmpMappings = appModule.getCmpMappings();
        if (cmpMappings == null) {
            cmpMappings = new EntityMappings();
            cmpMappings.setVersion("1.0");
            appModule.setCmpMappings(cmpMappings);
        }

        // we process this one jar-file at a time...each contributing to the 
        // app mapping data 
        for (EjbModule ejbModule : appModule.getEjbModules()) {
            EjbJar ejbJar = ejbModule.getEjbJar();

            // scan for CMP entity beans and merge the data into the collective set 
            for (EnterpriseBean enterpriseBean : ejbJar.getEnterpriseBeans()) {
                if (isCmpEntity(enterpriseBean)) {
                    processEntityBean(ejbModule, cmpMappings, (EntityBean) enterpriseBean);
                }
            }


            Relationships relationships = ejbJar.getRelationships();
            if (relationships != null) {

                Map<String, Entity> entitiesByEjbName = new TreeMap<String,Entity>();
                for (Entity entity : cmpMappings.getEntity()) {
                    entitiesByEjbName.put(entity.getEjbName(), entity);
                }

                for (EjbRelation relation : relationships.getEjbRelation()) {
                    processRelationship(entitiesByEjbName, relation);
                }
            }

            // Let's warn the user about any declarations we didn't end up using
            // so there can be no misunderstandings.
            EntityMappings userMappings = getUserEntityMappings(ejbModule);
            for (Entity mapping : userMappings.getEntity()) {
                logger.warning("openejb-cmp-orm.xml mapping ignored: module="+ejbModule.getModuleId()+":  <entity class=\""+mapping.getClazz()+"\">");
            }

            for (MappedSuperclass mapping : userMappings.getMappedSuperclass()) {
                logger.warning("openejb-cmp-orm.xml mapping ignored: module="+ejbModule.getModuleId()+":  <mapped-superclass class=\""+mapping.getClazz()+"\">");
            }

        }

        if (!cmpMappings.getEntity().isEmpty()) {
            PersistenceUnit persistenceUnit = getCmpPersistenceUnit(appModule);

            persistenceUnit.getMappingFile().add("META-INF/openejb-cmp-generated-orm.xml");
            for (Entity entity : cmpMappings.getEntity()) {
                persistenceUnit.getClazz().add(entity.getClazz());
            }
        }

        // TODO: This should not be necessary, but having an empty <attributes/> tag
        // causes some of the unit tests to fail.  Not sure why.  Should be fixed.
        for (Entity entity : appModule.getCmpMappings().getEntity()) {
            if (entity.getAttributes().isEmpty()){
                entity.setAttributes(null);
            }
        }
        return appModule;
    }

    private PersistenceUnit getCmpPersistenceUnit(AppModule appModule) {
        // search for the cmp persistence unit
        PersistenceUnit persistenceUnit = null;
        for (PersistenceModule persistenceModule : appModule.getPersistenceModules()) {
            Persistence persistence = persistenceModule.getPersistence();
            for (PersistenceUnit unit : persistence.getPersistenceUnit()) {
                if (CMP_PERSISTENCE_UNIT_NAME.equals(unit.getName())) {
                    persistenceUnit = unit;
                    break;
                }

            }
        }
        // if not found create one
        if (persistenceUnit == null) {
            persistenceUnit = new PersistenceUnit();
            persistenceUnit.setName(CMP_PERSISTENCE_UNIT_NAME);
            persistenceUnit.setTransactionType(TransactionType.JTA);
            // Don't set default values here, let the autoconfig do that
            // persistenceUnit.setJtaDataSource("java:openejb/Resource/Default JDBC Database");
            // persistenceUnit.setNonJtaDataSource("java:openejb/Resource/Default Unmanaged JDBC Database");
            // todo paramterize this
            Properties properties = new Properties();
            properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true, Indexes=false, IgnoreErrors=true)");
            // properties.setProperty("openjpa.DataCache", "false");
            // properties.setProperty("openjpa.Log", "DefaultLevel=TRACE");
            persistenceUnit.setProperties(properties);

            Persistence persistence = new Persistence();
            persistence.setVersion("1.0");
            persistence.getPersistenceUnit().add(persistenceUnit);

            PersistenceModule persistenceModule = new PersistenceModule(appModule.getModuleId(), persistence);
            appModule.getPersistenceModules().add(persistenceModule);
        }
        return persistenceUnit;
    }

    private boolean hasCmpEntities(AppModule appModule) {
        for (EjbModule ejbModule : appModule.getEjbModules()) {
            for (EnterpriseBean bean : ejbModule.getEjbJar().getEnterpriseBeans()) {
                if (isCmpEntity(bean)) return true;
            }
        }
        return false;
    }

    private static boolean isCmpEntity(EnterpriseBean bean) {
        return bean instanceof EntityBean && ((EntityBean) bean).getPersistenceType() == PersistenceType.CONTAINER;
    }

    private void processRelationship(Map<String, Entity> entitiesByEjbName, EjbRelation relation) throws OpenEJBException {
        List<EjbRelationshipRole> roles = relation.getEjbRelationshipRole();
        // if we don't have two roles, the relation is bad so we skip it
        if (roles.size() != 2) {
            return;
        }

        // get left entity
        EjbRelationshipRole leftRole = roles.get(0);
        RelationshipRoleSource leftRoleSource = leftRole.getRelationshipRoleSource();
        String leftEjbName = leftRoleSource == null ? null : leftRoleSource.getEjbName();
        Entity leftEntity = entitiesByEjbName.get(leftEjbName);

        // get right entity
        EjbRelationshipRole rightRole = roles.get(1);
        RelationshipRoleSource rightRoleSource = rightRole.getRelationshipRoleSource();
        String rightEjbName = rightRoleSource == null ? null : rightRoleSource.getEjbName();
        Entity rightEntity = entitiesByEjbName.get(rightEjbName);

        // neither left or right have a mapping which is fine
        if (leftEntity == null && rightEntity == null) {
            return;
        }
        // left not found?
        if (leftEntity == null) {
            throw new OpenEJBException("Role source " + leftEjbName + " defined in relationship role " +
                    relation.getEjbRelationName() + "::" + leftRole.getEjbRelationshipRoleName() + " not found");
        }
        // right not found?
        if (rightEntity == null) {
            throw new OpenEJBException("Role source " + rightEjbName + " defined in relationship role " +
                    relation.getEjbRelationName() + "::" + rightRole.getEjbRelationshipRoleName() + " not found");
        }

        final Attributes rightAttributes = rightEntity.getAttributes();
        Map<String, RelationField> rightRelationships = rightAttributes.getRelationshipFieldMap();
        final Attributes leftAttributes = leftEntity.getAttributes();
        Map<String, RelationField> leftRelationships = leftAttributes.getRelationshipFieldMap();

        String leftFieldName = null;
        boolean leftSynthetic = false;
        if (leftRole.getCmrField() != null) {
            leftFieldName = leftRole.getCmrField().getCmrFieldName();
        } else {
            leftFieldName = rightEntity.getName() + "_" + rightRole.getCmrField().getCmrFieldName();
            leftSynthetic = true;
        }
        boolean leftIsOne = leftRole.getMultiplicity() == Multiplicity.ONE;

        String rightFieldName = null;
        boolean rightSynthetic = false;
        if (rightRole.getCmrField() != null) {
            rightFieldName = rightRole.getCmrField().getCmrFieldName();
        } else {
            rightFieldName = leftEntity.getName() + "_" + leftRole.getCmrField().getCmrFieldName();
            rightSynthetic = true;
        }
        boolean rightIsOne = rightRole.getMultiplicity() == Multiplicity.ONE;

        if (leftIsOne && rightIsOne) {
            //
            // one-to-one
            //

            // left
            OneToOne leftOneToOne = null;
            leftOneToOne = new OneToOne();
            leftOneToOne.setName(leftFieldName);
            leftOneToOne.setSyntheticField(leftSynthetic);
            setCascade(rightRole, leftOneToOne);
            addRelationship(leftOneToOne, leftRelationships, leftAttributes.getOneToOne());

            // right
            OneToOne rightOneToOne = null;
            rightOneToOne = new OneToOne();
            rightOneToOne.setName(rightFieldName);
            rightOneToOne.setSyntheticField(rightSynthetic);
            rightOneToOne.setMappedBy(leftFieldName);
            setCascade(leftRole, rightOneToOne);
            addRelationship(rightOneToOne, rightRelationships, rightAttributes.getOneToOne());

            // link
            leftOneToOne.setRelatedField(rightOneToOne);
            rightOneToOne.setRelatedField(leftOneToOne);
        } else if (leftIsOne && !rightIsOne) {
            //
            // one-to-many
            //

            // left
            OneToMany leftOneToMany = null;
            leftOneToMany = new OneToMany();
            leftOneToMany.setName(leftFieldName);
            leftOneToMany.setSyntheticField(leftSynthetic);
            leftOneToMany.setMappedBy(rightFieldName);
            setCascade(rightRole, leftOneToMany);
            addRelationship(leftOneToMany, leftRelationships, leftAttributes.getOneToMany());

            // right
            ManyToOne rightManyToOne = null;
            rightManyToOne = new ManyToOne();
            rightManyToOne.setName(rightFieldName);
            rightManyToOne.setSyntheticField(rightSynthetic);
            setCascade(leftRole, rightManyToOne);
            addRelationship(rightManyToOne, rightRelationships, rightAttributes.getManyToOne());

            // link
            leftOneToMany.setRelatedField(rightManyToOne);
            rightManyToOne.setRelatedField(leftOneToMany);
        } else if (!leftIsOne && rightIsOne) {
            //
            // many-to-one
            //

            // left
            ManyToOne leftManyToOne = null;
            leftManyToOne = new ManyToOne();
            leftManyToOne.setName(leftFieldName);
            leftManyToOne.setSyntheticField(leftSynthetic);
            setCascade(rightRole, leftManyToOne);
            addRelationship(leftManyToOne, leftRelationships, leftAttributes.getManyToOne());

            // right
            OneToMany rightOneToMany = null;
            rightOneToMany = new OneToMany();
            rightOneToMany.setName(rightFieldName);
            rightOneToMany.setSyntheticField(rightSynthetic);
            rightOneToMany.setMappedBy(leftFieldName);
            setCascade(leftRole, rightOneToMany);
            addRelationship(rightOneToMany, rightRelationships, rightAttributes.getOneToMany());

            // link
            leftManyToOne.setRelatedField(rightOneToMany);
            rightOneToMany.setRelatedField(leftManyToOne);
        } else if (!leftIsOne && !rightIsOne) {
            //
            // many-to-many
            //

            // left
            ManyToMany leftManyToMany = null;
            leftManyToMany = new ManyToMany();
            leftManyToMany.setName(leftFieldName);
            leftManyToMany.setSyntheticField(leftSynthetic);
            setCascade(rightRole, leftManyToMany);
            addRelationship(leftManyToMany, leftRelationships, leftAttributes.getManyToMany());

            // right
            ManyToMany rightManyToMany = null;
            rightManyToMany = new ManyToMany();
            rightManyToMany.setName(rightFieldName);
            rightManyToMany.setSyntheticField(rightSynthetic);
            rightManyToMany.setMappedBy(leftFieldName);
            setCascade(leftRole, rightManyToMany);
            addRelationship(rightManyToMany, rightRelationships, rightAttributes.getManyToMany());

            // link
            leftManyToMany.setRelatedField(rightManyToMany);
            rightManyToMany.setRelatedField(leftManyToMany);
        }
    }

    private <R extends RelationField> R addRelationship(R relationship, Map<String, RelationField> existing, List<R> relationships) {
        R r = null;

        try {
            r = (R) existing.get(relationship.getKey());
        } catch (ClassCastException e) {
            return relationship;
        }

        if (r == null){
            r = relationship;
            relationships.add(relationship);
        }

        return r;
    }

    private void processEntityBean(EjbModule ejbModule, EntityMappings entityMappings, EntityBean bean) {
        // try to add a new persistence-context-ref for cmp
        if (!addPersistenceContextRef(bean)) {
            // Bean already has a persistence-context-ref for cmp
            // which means it has a mapping, so skip this bean
            return;
        }

        // get the real bean class 
        Class ejbClass = loadClass(ejbModule.getClassLoader(), bean.getEjbClass());
        // and generate a name for the subclass that will be generated and handed to the JPA 
        // engine as the managed class. 
        String jpaEntityClassName = CmpUtil.getCmpImplClassName(bean.getAbstractSchemaName(), ejbClass.getName());

        // We don't use this mapping directly, instead we pull entries from it
        // the reason being is that we intend to support mappings that aren't
        // exactly correct.  i.e. users should be able to write mappings completely
        // ignorant of the fact that we subclass.  The fact that we subclass means
        // these user supplied mappings might need to be adjusted as the jpa orm.xml
        // file is extremely subclass/supperclass aware and mappings specified in it
        // need to be spot on.
        EntityMappings userMappings = getUserEntityMappings(ejbModule);

        // Look for any existing mapped superclass mappings
        for (Class clazz = ejbClass; clazz != null; clazz = clazz.getSuperclass()){

            MappedSuperclass mappedSuperclass = removeMappedSuperclass(userMappings, clazz.getName());

            // We're going to assume that if they bothered to map a superclass
            // that the mapping is correct.  Copy it from their mappings to ours
            if (mappedSuperclass != null){
                entityMappings.getMappedSuperclass().add(mappedSuperclass);
            }
        }

        // Look for an existing mapping using the openejb generated subclass name
        Entity entity = removeEntity(userMappings, jpaEntityClassName);

        // DMB: For the first iteration, we're not going to allow
        // anything other than the ugly mapping file we generate.
        // So if they supplied an entity, it better be correct
        // because we are going to ignore all other xml metadata.
        if (entity != null) {
            // XmlMetadataComplete is an OpenEJB specific flag that
            // tells all other legacy descriptor converters to keep
            // their hands off.
            entity.setXmlMetadataComplete(true);

            entityMappings.getEntity().add(entity);

            return;
        }

// This section is an in progress TODO
//        if (entity == null){
//            entity = removeEntity(userMappings, ejbClass.getName());
//            // OVERWRITE: class: impl class name
//            if (entity != null) {
//                entity.setClazz(jpaEntityClassName);
//
//                if (Modifier.isAbstract(ejbClass.getModifiers())){
//                    // This is a CMP2 bean and we allowed the user to
//                    // define it via the orm.xml file as an <entity>
//                    // We need to split this definition.  We need
//                    // an <entity> definition for the generated subclass
//                    // and a <mapped-superclass> for the bean class
//
//
//                }
//            }
//        }

        if (entity == null){
            entity = new Entity(jpaEntityClassName);
        }

        // Aggressively add an "Attributes" instance so we don't
        // have to check for null everywhere.
        if (entity.getAttributes() == null){
            entity.setAttributes(new Attributes());
        }

        // add the entity
        entityMappings.getEntity().add(entity);

        // OVERWRITE: description: contains the name of the entity bean
        entity.setDescription(ejbModule.getModuleId() + "#" + bean.getEjbName());


        // PRESERVE has queries: name: the name of the entity in queries
        String entityName = bean.getAbstractSchemaName();
        entity.setName(entityName);
        entity.setEjbName(bean.getEjbName());



        ClassLoader classLoader = ejbModule.getClassLoader();
        if (bean.getCmpVersion() == CmpVersion.CMP2) {
            mapClass2x(entity, bean, classLoader);
        } else {
            // map the cmp class, but if we are using a mapped super class, generate attribute-override instead of id and basic
            Collection<MappedSuperclass> mappedSuperclasses = mapClass1x(bean.getEjbClass(), entity, bean, classLoader);

            for (MappedSuperclass mappedSuperclass : mappedSuperclasses) {
                entityMappings.getMappedSuperclass().add(mappedSuperclass);
            }
        }

        // process queries
        for (Query query : bean.getQuery()) {
            NamedQuery namedQuery = new NamedQuery();
            QueryMethod queryMethod = query.getQueryMethod();

            // todo deployment id could change in one of the later conversions... use entity name instead, but we need to save it off
            StringBuilder name = new StringBuilder();
            name.append(entityName).append(".").append(queryMethod.getMethodName());
            if (queryMethod.getMethodParams() != null && !queryMethod.getMethodParams().getMethodParam().isEmpty()) {
                name.append('(');
                boolean first = true;
                for (String methodParam : queryMethod.getMethodParams().getMethodParam()) {
                    if (!first) name.append(",");
                    name.append(methodParam);
                    first = false;
                }
                name.append(')');
            }
            namedQuery.setName(name.toString());

            namedQuery.setQuery(query.getEjbQl());
            entity.getNamedQuery().add(namedQuery);
        }
        // todo: there should be a common interface between ejb query object and openejb query object
        OpenejbJar openejbJar = ejbModule.getOpenejbJar();
        EjbDeployment ejbDeployment = openejbJar.getDeploymentsByEjbName().get(bean.getEjbName());
        if (ejbDeployment != null) {
            for (org.apache.openejb.jee.oejb3.Query query : ejbDeployment.getQuery()) {
                NamedQuery namedQuery = new NamedQuery();
                org.apache.openejb.jee.oejb3.QueryMethod queryMethod = query.getQueryMethod();

                // todo deployment id could change in one of the later conversions... use entity name instead, but we need to save it off
                StringBuilder name = new StringBuilder();
                name.append(entityName).append(".").append(queryMethod.getMethodName());
                if (queryMethod.getMethodParams() != null && !queryMethod.getMethodParams().getMethodParam().isEmpty()) {
                    name.append('(');
                    boolean first = true;
                    for (String methodParam : queryMethod.getMethodParams().getMethodParam()) {
                        if (!first) name.append(",");
                        name.append(methodParam);
                        first = false;
                    }
                    name.append(')');
                }
                namedQuery.setName(name.toString());

                namedQuery.setQuery(query.getObjectQl());
                entity.getNamedQuery().add(namedQuery);
            }
        }
    }

    private Entity removeEntity(EntityMappings userMappings, String className) {
        Entity entity;

        entity = userMappings.getEntityMap().get(className);
        if (entity != null){
            userMappings.getEntityMap().remove(entity.getKey());
        }
        return entity;
    }

    private MappedSuperclass removeMappedSuperclass(EntityMappings userMappings, String className) {
        MappedSuperclass mappedSuperclass;

        mappedSuperclass = userMappings.getMappedSuperclassMap().get(className);
        if (mappedSuperclass != null){
            userMappings.getMappedSuperclassMap().remove(mappedSuperclass.getKey());
        }
        return mappedSuperclass;
    }

    private EntityMappings getUserEntityMappings(EjbModule ejbModule) {
        Object o = ejbModule.getAltDDs().get("openejb-cmp-orm.xml");
        if (o instanceof EntityMappings) {
            return (EntityMappings) o;
        }
        return new EntityMappings();
    }

    private void mapClass2x(Mapping mapping, EntityBean bean, ClassLoader classLoader) {
        Set<String> allFields = new TreeSet<String>();
        for (CmpField cmpField : bean.getCmpField()) {
            allFields.add(cmpField.getFieldName());
        }

        // Add the cmp-field declarations for all the cmp fields that
        // weren't explicitly declared in the ejb-jar.xml
        try {
            Class<?> beanClass = classLoader.loadClass(bean.getEjbClass());
            for (Method method : beanClass.getMethods()) {
                if (!Modifier.isAbstract(method.getModifiers())) continue;
                if (method.getParameterTypes().length != 0) continue;
                if (method.getReturnType().equals(Void.TYPE)) continue;

                // Skip relationships: anything of type EJBLocalObject or Collection
                if (EJBLocalObject.class.isAssignableFrom(method.getReturnType())) continue;
                if (Collection.class.isAssignableFrom(method.getReturnType())) continue;
                if (Map.class.isAssignableFrom(method.getReturnType())) continue;

                String name = method.getName();

                if (name.startsWith("get")){
                    name = name.substring("get".length(), name.length());
                } else if (name.startsWith("is")){
                    // Only add this if the return type from an "is" method 
                    // boolean. 
                    if (method.getReturnType() == Boolean.TYPE) {
                        name = name.substring("is".length(), name.length());
                    }
                    else { 
                        // not an acceptable "is" method. 
                        continue; 
                    }
                } else continue;

                name = Strings.lcfirst(name);
                if (!allFields.contains(name)){
                    allFields.add(name);
                    bean.addCmpField(name);
                }
            }
        } catch (ClassNotFoundException e) {
            // class was already loaded in validation phase
        }

        //
        // id: the primary key
        //
        Set<String> primaryKeyFields = new HashSet<String>();
        if (bean.getPrimkeyField() != null) {
            String fieldName = bean.getPrimkeyField();
            Field field = new Id(fieldName);
            mapping.addField(field);
            primaryKeyFields.add(fieldName);
        } else if ("java.lang.Object".equals(bean.getPrimKeyClass())) {
            String fieldName = "OpenEJB_pk";
            Id field = new Id(fieldName);
            field.setGeneratedValue(new GeneratedValue(GenerationType.AUTO));
            mapping.addField(field);
            primaryKeyFields.add(fieldName);
        } else if (bean.getPrimKeyClass() != null) {
            Class<?> pkClass = null;
            try {
                pkClass = classLoader.loadClass(bean.getPrimKeyClass());
                mapping.setIdClass(new IdClass(bean.getPrimKeyClass()));
                for (java.lang.reflect.Field pkField : pkClass.getFields()) {
                    String pkFieldName = pkField.getName();
                    int modifiers = pkField.getModifiers();
                    if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && allFields.contains(pkFieldName)) {
                        Field field = new Id(pkFieldName);
                        mapping.addField(field);
                        primaryKeyFields.add(pkFieldName);
                    }
                }
            } catch (ClassNotFoundException e) {
                // todo throw exception
            }
        }

        //
        // basic: cmp-fields
        //
        for (CmpField cmpField : bean.getCmpField()) {
            if (!primaryKeyFields.contains(cmpField.getFieldName())) {
                Field field = new Basic(cmpField.getFieldName());
                mapping.addField(field);
            }
        }
    }

    private Collection<MappedSuperclass> mapClass1x(String ejbClassName, Mapping mapping, EntityBean bean, ClassLoader classLoader) {
        Class ejbClass = loadClass(classLoader, ejbClassName);

        // build a set of all field names
        Set<String> allFields = new TreeSet<String>();
        for (CmpField cmpField : bean.getCmpField()) {
            allFields.add(cmpField.getFieldName());
        }

        // build a map from the field name to the super class that contains that field
        Map<String, MappedSuperclass> superclassByField = mapFields(ejbClass, allFields);
        //
        // id: the primary key
        //
        Set<String> primaryKeyFields = new HashSet<String>();
        if (bean.getPrimkeyField() != null) {
            String fieldName = bean.getPrimkeyField();
            MappedSuperclass superclass = superclassByField.get(fieldName);
            if (superclass == null) {
                throw new IllegalStateException("Primary key field " + fieldName + " is not defined in class " + ejbClassName + " or any super classes");
            }
            superclass.addField(new Id(fieldName));
            mapping.addField(new AttributeOverride(fieldName));
            primaryKeyFields.add(fieldName);
        } else if ("java.lang.Object".equals(bean.getPrimKeyClass())) {
            String fieldName = "OpenEJB_pk";
            Id field = new Id(fieldName);
            field.setGeneratedValue(new GeneratedValue(GenerationType.AUTO));
            mapping.addField(field);
        } else if (bean.getPrimKeyClass() != null) {
            Class<?> pkClass = null;
            try {
                pkClass = classLoader.loadClass(bean.getPrimKeyClass());
                MappedSuperclass superclass = null;
                for (java.lang.reflect.Field pkField : pkClass.getFields()) {
                    String fieldName = pkField.getName();
                    int modifiers = pkField.getModifiers();
                    if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && allFields.contains(fieldName)) {
                        superclass = superclassByField.get(fieldName);
                        if (superclass == null) {
                            throw new IllegalStateException("Primary key field " + fieldName + " is not defined in class " + ejbClassName + " or any super classes");
                        }
                        superclass.addField(new Id(fieldName));
                        mapping.addField(new AttributeOverride(fieldName));
                        primaryKeyFields.add(fieldName);
                    }
                }
                if (superclass != null) {
                    superclass.setIdClass(new IdClass(bean.getPrimKeyClass()));
                }
            } catch (ClassNotFoundException e) {
                // todo throw exception
            }
        }

        //
        // basic: cmp-fields
        //
        for (CmpField cmpField : bean.getCmpField()) {
            String fieldName = cmpField.getFieldName();
            if (!primaryKeyFields.contains(fieldName)) {
                MappedSuperclass superclass = superclassByField.get(fieldName);
                if (superclass == null) {
                    throw new IllegalStateException("Primary key field " + fieldName + " is not defined in class " + ejbClassName + " or any super classes");
                }
                superclass.addField(new Basic(fieldName));
                mapping.addField(new AttributeOverride(fieldName));
            }
        }

        return new HashSet<MappedSuperclass>(superclassByField.values());
    }

    private static Class loadClass(ClassLoader classLoader, String className) {
        Class ejbClass = null;
        try {
            ejbClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        return ejbClass;
    }

    private Map<String, MappedSuperclass> mapFields(Class clazz, Set<String> persistantFields) {
        persistantFields = new TreeSet<String>(persistantFields);
        Map<String,MappedSuperclass> fields = new TreeMap<String,MappedSuperclass>();

        while (!persistantFields.isEmpty() && !clazz.equals(Object.class)) {
            MappedSuperclass superclass = new MappedSuperclass(clazz.getName());
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                String fieldName = field.getName();
                if (persistantFields.contains(fieldName)) {
                    fields.put(fieldName, superclass);
                    persistantFields.remove(fieldName);
                } else if (!ENHANCEED_FIELDS.contains(fieldName)){
                    Transient transientField = new Transient(fieldName);
                    superclass.addField(transientField);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    private boolean addPersistenceContextRef(EntityBean bean) {
        // if a ref is already defined, skip this bean
        if (bean.getPersistenceContextRefMap().containsKey(JpaCmpEngine.CMP_PERSISTENCE_CONTEXT_REF_NAME)) return false;

        PersistenceContextRef persistenceContextRef = new PersistenceContextRef();
        persistenceContextRef.setName(JpaCmpEngine.CMP_PERSISTENCE_CONTEXT_REF_NAME);
        persistenceContextRef.setPersistenceUnitName(CMP_PERSISTENCE_UNIT_NAME);
        bean.getPersistenceContextRef().add(persistenceContextRef);
        return true;
    }

    private void setCascade(EjbRelationshipRole role, RelationField field) {
        if (role.getCascadeDelete()) {
            CascadeType cascadeType = new CascadeType();
            cascadeType.setCascadeAll(true);
            field.setCascade(cascadeType);
        }
    }

    public static interface Member {
        Class getDeclaringClass();

        String getName();

        Class getType();
    }

    public static class MethodMember implements Member {
        private final Method setter;

        public MethodMember(Method method) {
            this.setter = method;
        }

        public Class getType() {
            return setter.getParameterTypes()[0];
        }

        public Class getDeclaringClass() {
            return setter.getDeclaringClass();
        }

        public String getName() {
            StringBuilder name = new StringBuilder(setter.getName());

            // remove 'set'
            name.delete(0, 3);

            // lowercase first char
            name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

            return name.toString();
        }

        public String toString() {
            return setter.toString();
        }
    }

    public static class FieldMember implements Member {
        private final java.lang.reflect.Field field;

        public FieldMember(java.lang.reflect.Field field) {
            this.field = field;
        }

        public Class getType() {
            return field.getType();
        }

        public String toString() {
            return field.toString();
        }

        public Class getDeclaringClass() {
            return field.getDeclaringClass();
        }

        public String getName() {
            return field.getName();
        }
    }

}