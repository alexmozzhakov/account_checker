# Account Checker
Checks for log/pass combinations for user with User-Agent substitution for .db

An included example of such checker:
```
class SimpleAuthChecker : Checker {
    override fun check(login: String, password: String, ua: String): Boolean {
        FuelManager.instance.baseHeaders = mapOf("User-agent" to ua)
        val (_, _, result) = "http://localhost:7000/login"
                                  .httpPost(listOf("login" to login, "pass" to password)).responseString()
        return result.component1().equals("login successful")
    }
}
```
for [simple_auth](https://github.com/alexmozzhakov/simple_auth) website
