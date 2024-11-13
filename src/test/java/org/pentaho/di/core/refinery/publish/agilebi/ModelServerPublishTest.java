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


package org.pentaho.di.core.refinery.publish.agilebi;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.publish.model.DataSourceAclModel;
import org.pentaho.di.core.refinery.publish.util.JAXBUtils;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Rowell Belen
 */
public class ModelServerPublishTest {

  private ModelServerPublish modelServerPublish;
  private ModelServerPublish modelServerPublishSpy;
  private DatabaseMeta databaseMeta;
  private DatabaseInterface databaseInterface;
  private IDatabaseType databaseType;
  private DatabaseTypeHelper databaseTypeHelper;
  private DatabaseConnection databaseConnection;
  private BiServerConnection connection;
  private Properties attributes;
  private Client client;
  private Response clientResponse;
  private boolean overwrite;
  private LogChannelInterface logChannel;

  @Before
  public void setup() {

    overwrite = false;
    databaseMeta = mock( DatabaseMeta.class );
    connection = new BiServerConnection();
    connection.setUserId( "admin" );
    connection.setPassword( "password" );
    connection.setUrl( "http://localhost:8080/pentaho" );

    client = mock( Client.class );
    databaseInterface = mock( DatabaseInterface.class );
    databaseType = mock( IDatabaseType.class );
    databaseTypeHelper = mock( DatabaseTypeHelper.class );
    databaseConnection = mock( DatabaseConnection.class );
    attributes = mock( Properties.class );
    clientResponse = mock( Response.class );
    logChannel = mock( LogChannelInterface.class );

    modelServerPublish = new ModelServerPublish( logChannel );
    modelServerPublishSpy = spy( modelServerPublish );

    // mock responses
    doReturn( client ).when( modelServerPublishSpy ).getClient();

    // inject dependencies
    modelServerPublishSpy.setForceOverwrite( overwrite );
    modelServerPublishSpy.setBiServerConnection( connection );
    modelServerPublishSpy.setDatabaseMeta( databaseMeta );
  }

  @Ignore
  @Test
  public void testConnectionnameExists() throws Exception {
    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    DatabaseConnection dbConnection1 = new DatabaseConnection();
    dbConnection1.setName( "test" );
    String json = JAXBUtils.marshallToJson( dbConnection1 );

    // check null connection name
    assertNull( modelServerPublishSpy.connectionNameExists( null ) );

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpGet( any( Invocation.Builder.class ) );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpGet( any( Invocation.Builder.class ) );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // check invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    assertNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // valid
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.readEntity( String.class ) ).thenReturn( json );
    assertNotNull( modelServerPublishSpy.connectionNameExists( "test" ) );

    // valid
    String testStr = "クイズ";
    modelServerPublishSpy.connectionNameExists( testStr );
    verify( modelServerPublishSpy ).constructAbsoluteUrl( URLEncoder.encode( testStr, "UTF-8" ) );
  }

  @Test
  public void testConstructAbsoluteUrl() throws Exception {
    String connectionName = "local pentaho";
    String actual = modelServerPublishSpy.constructAbsoluteUrl( connectionName );
    String expected
      = "http://localhost:8080/pentaho/plugin/data-access/api/connection/getresponse?name=local%20pentaho";
    assertEquals( expected, actual );
  }

  @Test
  public void testPublishDataSource() throws Exception {

    doReturn( databaseInterface ).when( databaseMeta ).getDatabaseInterface();

    final HashMap<String, String> extraOptions = new HashMap<String, String>();
    final String username = "username";
    final String password = "password";
    final String dbName = "dbName";
    final String dbPort = "dbPort";
    final String hostname = "hostname";

    doReturn( extraOptions ).when( databaseMeta ).getExtraOptions();
    doReturn( username ).when( databaseMeta ).getUsername();
    doReturn( username ).when( databaseMeta ).environmentSubstitute( username );
    doReturn( password ).when( databaseMeta ).getPassword();
    doReturn( password ).when( databaseMeta ).environmentSubstitute( password );
    doReturn( dbName ).when( databaseInterface ).getDatabaseName();
    doReturn( dbName ).when( databaseMeta ).environmentSubstitute( dbName );
    doReturn( dbPort ).when( databaseMeta ).environmentSubstitute( dbPort );
    doReturn( hostname ).when( databaseMeta ).getHostname();
    doReturn( hostname ).when( databaseMeta ).environmentSubstitute( hostname );

    doReturn( attributes ).when( databaseInterface ).getAttributes();
    doReturn( dbPort ).when( attributes ).getProperty( "PORT_NUMBER" );
    doReturn( "Y" ).when( attributes ).getProperty( "FORCE_IDENTIFIERS_TO_LOWERCASE" );
    doReturn( "Y" ).when( attributes ).getProperty( "QUOTE_ALL_FIELDS" );
    doReturn( databaseType ).when( modelServerPublishSpy ).getDatabaseType( databaseInterface );

    try {
      modelServerPublishSpy.publishDataSource( true, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
    verify( modelServerPublishSpy ).updateConnection( argThat( new ArgumentMatcher<DatabaseConnection>() {
      @Override public boolean matches( DatabaseConnection o ) {
        DatabaseConnection db = (DatabaseConnection) o;
        return db.getUsername().equals( username )
            && db.getPassword().equals( password )
            && db.getDatabaseName().equals( dbName )
            && db.getDatabasePort().equals( dbPort )
            && db.getHostname().equals( hostname )
            && db.isForcingIdentifiersToLowerCase()
            && db.isQuoteAllFields()
            && db.getAccessType().equals( DatabaseAccessType.NATIVE )
            && db.getExtraOptions().equals( databaseMeta.getExtraOptions() )
            && db.getDatabaseType().equals( databaseType );
      }
    } ), anyBoolean() );

    doReturn( "N" ).when( attributes ).get( anyString() );
    try {
      modelServerPublishSpy.publishDataSource( false, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
  }

  @Test
  public void testPublishDataSourceEnvironmentSubstitute() throws Exception {

    doReturn( databaseInterface ).when( databaseMeta ).getDatabaseInterface();
    doReturn( "${USER_NAME}" ).when( databaseMeta ).getUsername();
    doReturn( "${USER_PASSWORD}" ).when( databaseMeta ).getPassword();
    doReturn( "${HOST_NAME}" ).when( databaseMeta ).getHostname();
    doReturn( "SubstitutedUser" ).when( databaseMeta ).environmentSubstitute( "${USER_NAME}" );
    doReturn( "SubstitutedHostName" ).when( databaseMeta ).environmentSubstitute( "${HOST_NAME}" );
    doReturn( "SubstitutedPassword" ).when( databaseMeta ).environmentSubstitute( "${USER_PASSWORD}" );
    doReturn( attributes ).when( databaseInterface ).getAttributes();
    doReturn( "${DB_PORT}" ).when( attributes ).getProperty( anyString() );
    doReturn( "8080" ).when( databaseMeta ).environmentSubstitute( "${DB_PORT}" );
    doReturn( databaseType ).when( modelServerPublishSpy ).getDatabaseType( databaseInterface );

    try {
      modelServerPublishSpy.publishDataSource( true, "id" );
    } catch ( KettleException e ) {
      // Will hit this block
    }
    verify( modelServerPublishSpy ).updateConnection( argThat( new ArgumentMatcher<DatabaseConnection>( ) {
        @Override
        public boolean matches( DatabaseConnection o ) {
          DatabaseConnection db = (DatabaseConnection) o;
          return db.getUsername( ).equals( "SubstitutedUser" )
                  && db.getHostname( ).equals( "SubstitutedHostName" )
                  && db.getPassword( ).equals( "SubstitutedPassword" )
                  && db.getDatabasePort( ).equals( "8080" );
        }
      } ), anyBoolean() );

  }



  @Test
  public void testGetDatabaseType() throws Exception {
    doReturn( "" ).when( databaseInterface ).getPluginId();
    assertNull( modelServerPublishSpy.getDatabaseType( databaseInterface ) );
  }

  @Test
  public void testCanPublishDatabaseTypesThatAreNotAvailableInThisClassloader() throws Exception {
    doReturn( "ORACLE" ).when( databaseInterface ).getPluginId();  //Oracle driver shouldn't be available for unit tests
    assertEquals( "Oracle", modelServerPublishSpy.getDatabaseType( databaseInterface ).getName() );
  }

  @Test
  public void testGetClient() throws Exception {

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    Client client1 = modelServerPublishSpy.getClient();
    Client client2 = modelServerPublishSpy.getClient();
    assertEquals( client1, client2 ); // assert same instance
  }

  @Test
  public void testHttpPost() throws Exception {
    modelServerPublishSpy.httpPost( mock( Invocation.Builder.class ), mock( Entity.class ) );
  }

  @Ignore
  @Test
  public void testUpdateConnection() throws Exception {

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPost( any( Invocation.Builder.class ), any( Entity.class ) );
    boolean success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPost( any( Invocation.Builder.class ), any( Entity.class ) );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertFalse( success );

    // valid
    when( clientResponse.getStatus() ).thenReturn( 200 );
    success = modelServerPublishSpy.updateConnection( databaseConnection, false );
    assertTrue( success );
  }

  @Test
  public void testDeleteConnectionEncodesName() throws Exception {
    modelServerPublishSpy.deleteConnection( "some name" );
    verify( client ).target( "http://localhost:8080/pentaho/plugin/data-access/api/connection/deletebyname?name=some+name" );
  }

  @Ignore
  @Test
  public void testPublishMondrianSchema() throws Exception {

    InputStream mondrianFile = mock( InputStream.class );
    String catalogName = "Catalog";
    String datasourceInfo = "Test";
    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPost( any( Invocation.Builder.class ), any( Entity.class ) );
    int status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPost( any( Invocation.Builder.class ), any( Entity.class ) );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // valid status, invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.readEntity( String.class ) ).thenReturn( "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // valid status, catalog exists
    when( clientResponse.readEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_CATALOG_EXISTS + "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_CATALOG_EXISTS, status );

    // success
    when( clientResponse.readEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishMondrianSchema( mondrianFile, catalogName, datasourceInfo, true );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );
  }

  private ArgumentMatcher<FormDataMultiPart> matchPart(
      final String parameters, final Object inputStream, final String catalog,
      final String overwrite, final String xmlaEnabled ) {
    return new ArgumentMatcher<FormDataMultiPart>() {
      @Override public boolean matches( final FormDataMultiPart item ) {
        FormDataMultiPart part = (FormDataMultiPart) item;
        List<BodyPart> bodyParts = part.getBodyParts();
        return bodyParts.size() == 5
          && bodyParts.get( 0 ).getEntity().equals( parameters )
          && bodyParts.get( 1 ).getEntity().equals( inputStream )
          && bodyParts.get( 2 ).getEntity().equals( catalog )
          && bodyParts.get( 3 ).getEntity().equals( overwrite )
          && bodyParts.get( 4 ).getEntity().equals( xmlaEnabled );
      }
    };
  }

  private ArgumentMatcher<WebTarget> matchResource(final String expectedUri ) {
    return new ArgumentMatcher<WebTarget>() {
      @Override public boolean matches( final WebTarget item ) {
        WebTarget resource = (WebTarget) item;
        return resource.getUri().toString().equals( expectedUri );
      }
    };
  }

  @Ignore
  @Test
  public void testPublishMetaDataFile() throws Exception {

    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPut( any( Invocation.Builder.class ), any( Entity.class ) );
    int status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( Invocation.Builder.class ), any( Entity.class ) );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    modelServerPublishSpy.setForceOverwrite( true );

    // valid status, invalid payload
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.readEntity( String.class ) ).thenReturn( "" );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // success
    when( clientResponse.readEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // valid status, but throw error
    when( clientResponse.readEntity( String.class ) ).thenThrow( new RuntimeException() );
    status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );
  }


  @Ignore
  @Test
  public void testPublishMetaDataFileWithAcl() throws Exception {
    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    DataSourceAclModel aclModel = new DataSourceAclModel();
    aclModel.addUser( "testUser" );
    modelServerPublishSpy.setAclModel( aclModel );

    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( Invocation.Builder.class ), any( Entity.class ) );
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.readEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    int status = modelServerPublishSpy.publishMetaDataFile( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );
  }


  @Ignore
  @Test( expected = IllegalArgumentException.class )
  public void testPublishDsw() throws Exception {

    InputStream metadataFile = mock( InputStream.class );
    String domainId = "Test.xmi";

    doCallRealMethod().when( modelServerPublishSpy ).getClient();

    // check null response
    doReturn( null ).when( modelServerPublishSpy ).httpPut( any( Invocation.Builder.class ), any( Entity.class ) );
    int status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid clientResponse
    doReturn( clientResponse ).when( modelServerPublishSpy ).httpPut( any( Invocation.Builder.class ), any( Entity.class ) );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    // check invalid status
    when( clientResponse.getStatus() ).thenReturn( 404 );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_FAILED, status );

    modelServerPublishSpy.setForceOverwrite( true );

    // valid status - 200
    when( clientResponse.getStatus() ).thenReturn( 200 );
    when( clientResponse.readEntity( String.class ) ).thenReturn( ModelServerPublish.PUBLISH_SUCCESS + "" );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // valid status - 201
    when( clientResponse.getStatus() ).thenReturn( 201 );
    status = modelServerPublishSpy.publishDsw( metadataFile, domainId );
    assertEquals( ModelServerPublish.PUBLISH_SUCCESS, status );

    // throw exception
    domainId = "Test";
    modelServerPublishSpy.publishDsw( metadataFile, domainId );
  }
}
