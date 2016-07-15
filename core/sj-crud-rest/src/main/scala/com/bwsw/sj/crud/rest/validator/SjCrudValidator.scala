package com.bwsw.sj.crud.rest.validator

import java.io._
import java.util.jar.JarFile

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.RequestContext
import akka.stream.Materializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.service.GenericMongoService
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONObject, JSONTokener}

import scala.concurrent.{Await, ExecutionContextExecutor}

/**
  * Trait for validation of crud-rest-api
  * and contains common methods for routes
  *
  * Created: 06/04/2016
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudValidator {
  val logger: LoggingAdapter

  implicit val materializer: Materializer
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor

  val serializer: Serializer
  val fileMetadataDAO: GenericMongoService[FileMetadata]
  val storage: FileStorage
  val instanceDAO: GenericMongoService[Instance]
  val serviceDAO: GenericMongoService[Service]
  val streamDAO: GenericMongoService[SjStream]
  val providerDAO: GenericMongoService[Provider]
  val configService: GenericMongoService[ConfigSetting]
  val restHost: String
  val restPort: Int

  import com.bwsw.sj.common.ModuleConstants._
  import com.bwsw.sj.common.StreamConstants._

  /**
    * Getting entity from HTTP-request
    *
    * @param ctx - request context
    * @return - entity from http-request as string
    */
  def getEntityFromContext(ctx: RequestContext): String = {
    getEntityAsString(ctx.request.entity)
  }

  def getEntityAsString(entity: HttpEntity): String = {
    import scala.concurrent.duration._
    Await.result(entity.toStrict(1.second), 1.seconds).data.decodeString("UTF-8")
  }

  /**
    * Check String object
    *
    * @param value - input string
    * @return - boolean result of checking
    */
  def isEmptyOrNullString(value: String): Boolean = value == null || value.isEmpty

  /**
    * Check specification of uploading jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  def checkJarFile(jarFile: File) = {
    val json = getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      logger.debug(s"File specification.json not found in module jar ${jarFile.getName}.")
      throw new FileNotFoundException(s"Specification.json for ${jarFile.getName} is not found!")
    }
    schemaValidate(json, getClass.getClassLoader.getResourceAsStream("schema.json"))
    val specification = serializer.deserialize[Map[String, Any]](json)
    val moduleType = specification("module-type").asInstanceOf[String]
    if (moduleType.equals(outputStreamingType)) {
      val inputs = specification("inputs").asInstanceOf[Map[String, Any]]
      val inputTypes = inputs("types").asInstanceOf[List[String]]
      val inputCardinalites = inputs("cardinality").asInstanceOf[List[Int]]

      val outputs = specification("outputs").asInstanceOf[Map[String, Any]]
      val outputTypes = outputs("types").asInstanceOf[List[String]]
      val outputCardinalites = outputs("cardinality").asInstanceOf[List[Int]]

      if (inputTypes.length > 1 ||
        !inputTypes.head.equals(tStream) || (inputCardinalites.head != 1 || inputCardinalites(1) != 1) ||
        !(outputTypes.contains(jdbcOutput) || outputTypes.contains(esOutput)) ||
        (outputCardinalites.head != 1 || outputCardinalites(1) != 1)
      ) {
        throw new Exception("Specification.json for output-streaming has incorrect params!")
      }

      if (specification.get("entity-class").isEmpty) {
        throw new Exception("Specification.json for output-streaming hasn't 'entity-class' param!")
      }
    } else if (moduleType.equals(inputStreamingType)) {
      val inputs = specification("inputs").asInstanceOf[Map[String, Any]]
      val inputTypes = inputs("types").asInstanceOf[List[String]]
      val inputCardinalites = inputs("cardinality").asInstanceOf[List[Int]]

      val outputs = specification("outputs").asInstanceOf[Map[String, Any]]
      val outputTypes = outputs("types").asInstanceOf[List[String]]

      if ((inputTypes.size > 1 || !inputTypes.contains(input)
        || (inputCardinalites.head != 0 && inputCardinalites(1) != 0))
        || (outputTypes.size != 1 || !outputTypes.contains(tStream))) {
        throw new Exception("Specification.json for input-streaming has incorrect params!")
      }
    }

    specification
  }

  /**
    * Check specification of uploading custom jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  def checkCustomJarFile(jarFile: File) = {
    val json = getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      throw new FileNotFoundException(s"Specification.json for ${jarFile.getName} is not found!")
    }
    schemaValidate(json, getClass.getClassLoader.getResourceAsStream("customschema.json"))
    serializer.deserialize[Map[String, Any]](json)
  }

  /**
    * Return content of specification.json file from root of jar
    *
    * @param file - Input jar file
    * @return - json-string from specification.json
    */
  private def getSpecificationFromJar(file: File): String = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while(enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        try {
          var line = reader.readLine
          while (line != null) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        } finally {
          reader.close()
        }
      }
    }
    builder.toString()
  }

  /**
    * Validate json for such schema
    *
    * @param json - input json
    * @param schemaStream - schema
    * @return - true, if schema is valid
    */
  def schemaValidate(json: String, schemaStream: InputStream): Boolean = {
    if (schemaStream != null) {
      val rawSchema = new JSONObject(new JSONTokener(schemaStream))
      val schema = SchemaLoader.load(rawSchema)
      schema.validate(new JSONObject(json))
    } else {
      throw new Exception("Json schema for specification is not found")
    }
    true
  }

  /**
    * Check existing such type of modules
    *
    * @param typeName - name type of module
    * @return - true, if module type is exist, else false
    */
  def checkModuleType(typeName: String) = {
    moduleTypes.contains(typeName)
  }
}