import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.sql.DriverManager


class UserManager {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    fun getUsersList(): Array<User> {
        logger.info("Staring check...")
        val connection = DriverManager.getConnection("jdbc:sqlite:./accounts.db")
        val statement = connection.createStatement()
        statement.queryTimeout = 30  // set timeout to 30 sec.
        statement.executeUpdate("create table if not exists users (id INT, login TEXT, password TEXT, ua TEXT)")
        val countResultSet = statement.executeQuery("select count(DISTINCT id) from users")
        countResultSet.next()
        val uniqueCount = countResultSet.getInt(1)
        val users = Array(uniqueCount, { User() })
        countResultSet.close()
        logger.info("Found $uniqueCount users to check")
        val rs = statement.executeQuery("select * from users")
        while (rs.next()) {
            val userId = rs.getInt("id")
            val user = users[userId - 1]

            val login = rs.getString("login")
            val password = rs.getString("password")
            val userAgent = rs.getString("ua")
            if (login != null && login.isNotEmpty()) user.logins.add(login)
            if (password != null && password.isNotEmpty()) user.passwords.add(password)
            if (userAgent != null && userAgent.isNotEmpty()) user.ua = userAgent
        }
        rs.close()
        return users
    }
}

class User {

    var logins = ArrayList<String>()
    var passwords = ArrayList<String>()
    var ua = ""

    override fun toString(): String {
        return "logins:\n" + logins.toString() + "\npasswords:\n" + passwords + "\n\n"
    }
}

interface Checker {
    fun check(login: String, password: String, ua: String): Boolean
}

class SimpleAuthChecker : Checker {
    override fun check(login: String, password: String, ua: String): Boolean {
        FuelManager.instance.baseHeaders = mapOf("User-agent" to ua)
        val (_, _, result) = "http://localhost:7000/login".httpPost(listOf("login" to login, "pass" to password)).responseString() // result is Result<String, FuelError>
        return result.component1().equals("login successful")
    }

}

fun runCommand(string: String) {
    val r = Runtime.getRuntime()
    val p = r.exec(string)
    p.waitFor()
    val b = BufferedReader(InputStreamReader(p.inputStream)).use(BufferedReader::readText)
    println(b)
}

fun main(args: Array<String>) {
    val users = UserManager().getUsersList()
    val services: List<Checker> = listOf(SimpleAuthChecker())
    for (user in users) {
        user.logins.forEach { login ->
            user.passwords.forEach { password ->
                services.forEach {
                    printTestResult(it.check(login, password, user.ua))
                }
            }
        }
    }
}

fun printTestResult(result: Boolean) {
    println("login " + if (result) "success" else "fail")
}
