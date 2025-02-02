/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.refinery.publish.util;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;

import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;

import static org.junit.Assert.*;

/**
 * @author Rowell Belen
 */
public class ObjectUtilsTest {

  private ObjectUtils objectUtils;
  private BiServerConnection biServerConnection;

  @Before
  public void init() {

    objectUtils = new ObjectUtils();

    biServerConnection = new BiServerConnection();
    biServerConnection.setName( "default" );
    biServerConnection.setUserId( "admin" );
    biServerConnection.setPassword( "password" );
    biServerConnection.setUrl( "http://localhost:8080/pentaho/" );
  }

  @Test
  public void test() {
    assertNull( objectUtils.toXml( null ) );
    String xml = objectUtils.toXml( biServerConnection );
    assertTrue( StringUtils.contains( xml, "default" ) );
    assertTrue( StringUtils.contains( xml, "password" ) );

    objectUtils.logInfo( null );
    objectUtils.logInfo( xml );
  }

  @Test
  public void testDeepClone() {

    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setName( "default" );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setOverride( true );
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerModel );

    BiServerConnection clone = objectUtils.deepClone( biServerModel );
    assertEquals( clone.getName(), biServerModel.getName() );

    objectUtils.deepClone( null );
  }

  @Test
  public void testDeepClone2() {
    CreateDimensionKey cdk = new CreateDimensionKey();
    cdk.setDimension( "Some Dimension" );
    cdk.setName( "Some Name" );

    CreateAttribute ca = new CreateAttribute();
    ca.setName( "Attribute Name" );
    ca.setDimension( "Some Dimension" );
    ca.setDescription( "Some Description" );
    ca.setBusinessGroup( "Some Business Group" );
    ca.setGeoType( ModelAnnotation.GeoType.City );
    ca.setHierarchy( "Some hierarchy" );
    ca.setOrdinalField( "Some Ordinal Field" );
    ca.setParentAttribute( "Some Parent Attribute" );
    ca.setTimeFormat( "Some Time Format" );
    ca.setTimeType( ModelAnnotation.TimeType.TimeDays );
    ca.setUnique( true );

    ModelAnnotation<?> m1 = new ModelAnnotation<AnnotationType>( "f1", cdk );
    ModelAnnotation<?> m2 = new ModelAnnotation<AnnotationType>( "f2", ca );
    ModelAnnotationGroup mag = new ModelAnnotationGroup( m1, m2 );
    mag.setName( "mag" );

    ModelAnnotationGroup clone = objectUtils.deepClone( mag );
    assertEquals( clone.getName(), mag.getName() );
    assertEquals( clone.get( 0 ).getName(), mag.get( 0 ).getName() );
    assertTrue( clone.getName() != mag.getName() );
    assertTrue( clone.get( 0 ) != mag.get( 0 ) );
    assertTrue( clone.get( 0 ).getName() != mag.get( 0 ).getName() );
    
  }
}
