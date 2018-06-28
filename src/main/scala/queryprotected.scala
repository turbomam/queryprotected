import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.io.File
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties
import java.io.InputStreamReader
import java.io.Reader
import org.eclipse.rdf4j.rio._
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.OpenRDFException
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.model.Value
import java.io.PrintWriter
import org.eclipse.rdf4j.model.Literal._
import org.eclipse.rdf4j.model.Literal

object queryprotected {
  
  def main(args: Array[String]): Unit = 
  {
      var cxn: RepositoryConnection = null
      var repoManager: RemoteRepositoryManager = null
      var repository: Repository = null
      try
      {
          val graphDBMaterials: TurboGraphConnection = initializeGraph()
          cxn = graphDBMaterials.getConnection()
          repoManager = graphDBMaterials.getRepoManager()
          repository = graphDBMaterials.getRepository() 
          if (cxn == null) println("There was a problem initializing the graph. Please check your properties file for errors.")
          runQueryFromFile(cxn, retrievePropertyFromFile("inputQuery"))
      }
      finally
      {
          closeGraphConnection(cxn, repoManager, repository)
      }
}
  
  def runQueryFromFile(cxn: RepositoryConnection, inputQueryFile: String)
  {
      val query: String = new String(Files.readAllBytes(Paths.get(new File(inputQueryFile).getAbsolutePath())))
      println()
      println("About to run this query: ")
      println(query)
      println()
      
      val result: TupleQueryResult = querySparql(cxn, query).get
      val bindingNames: Array[Object] = result.getBindingNames().toArray
      val resultArray: ArrayBuffer[ArrayBuffer[Value]] = executeQuery(cxn, result, bindingNames, query)
      printQueryResults(resultArray, retrievePropertyFromFile("outputFile"), bindingNames)
  }
  
  def executeQuery(cxn: RepositoryConnection, result: TupleQueryResult, bindingNames: Array[Object], query: String): ArrayBuffer[ArrayBuffer[Value]] =
  {
      val resultList: ArrayBuffer[ArrayBuffer[Value]] = new ArrayBuffer[ArrayBuffer[Value]]
        
      while (result.hasNext())
      {
          val bindingset: BindingSet = result.next()
          val oneResult: ArrayBuffer[Value] = new ArrayBuffer[Value]
          for (a <- 0 to bindingNames.size - 1)
          {
              val varToCheck: String = bindingNames(a).toString
              val singleResult: Value = bindingset.getValue(varToCheck)
              oneResult += singleResult
          }
          resultList += oneResult
      }
      resultList
  }
  
  def printQueryResults(resultArray: ArrayBuffer[ArrayBuffer[Value]], output: String, bindingNames: Array[Object])
  {
      val bareLiterals = retrievePropertyFromFile("bareLiterals").toBoolean
      val triples2stdout = retrievePropertyFromFile("triples2stdout").toBoolean      
      val pw: PrintWriter = new PrintWriter(new File (output))
      for (name <- bindingNames) pw.print(name.toString + "\t")
      pw.println()
      if (resultArray.size == 0) println("No results.")
      else
      {
          println()
          println("Result size: " + resultArray.size)
          println("Query Results: ")
          for (singleResult <- resultArray)
          {
              for (variable <- singleResult)
              {
                if (bareLiterals && variable.isInstanceOf[Literal]) {
                  val varLit = variable.asInstanceOf[Literal]
                  val litLab = varLit.getLabel
                  if(triples2stdout) { print(litLab + " ") }
                  pw.print(litLab + "\t")
                } else {
                  if(triples2stdout) { print(variable + " ") }
                  pw.print(variable + "\t")
                }
              }
              if(triples2stdout) { println() }
              pw.println()
          }
      }
      pw.close()
  }
  
  def querySparql(cxn: RepositoryConnection, query: String): Option[TupleQueryResult] =
  {
      var result: Option[TupleQueryResult] = None : Option[TupleQueryResult]
      try 
      {
          val tupleQuery: TupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query)
          //convert tupleQuery into TupleQueryResult using built-in evaluate() function
          result = Some(tupleQuery.evaluate())
          result
      }
      catch
      {
          case e: OpenRDFException => println(e.toString)
          None
      }
  }
 
  def initializeGraph(): TurboGraphConnection =
  {
      val graphConnect: TurboGraphConnection = new TurboGraphConnection
      val repoManager: RemoteRepositoryManager = new RemoteRepositoryManager(retrievePropertyFromFile("serviceURL"))
      repoManager.setUsernameAndPassword(retrievePropertyFromFile("username"), retrievePropertyFromFile("password"))
      repoManager.initialize()
      val repository: Repository = repoManager.getRepository(retrievePropertyFromFile("repository"))
      val cxn: RepositoryConnection = repository.getConnection()
      graphConnect.setConnection(cxn)
      graphConnect.setRepoManager(repoManager)
      graphConnect.setRepository(repository)
      graphConnect
  }
  
  def retrievePropertyFromFile(property: String, file: String = "queryprotected_params.txt"): String =
  {
      val input: FileInputStream = new FileInputStream(file)
      val props: Properties = new Properties()
      props.load(input)
      input.close()
      props.getProperty(property)
  }
  
  def closeGraphConnection(cxn: RepositoryConnection, repoManager: RemoteRepositoryManager, repository: Repository)
  {
      if (cxn == null)
      {
          println("Connection to the repository is not active - could not be closed.")
      }
      else
      {
          cxn.close()
          repository.shutDown()
          repoManager.shutDown()
      }
  }
}
