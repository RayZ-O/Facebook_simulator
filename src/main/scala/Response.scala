import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsNumber
import spray.json.JsValue
import spray.json.DeserializationException

case class User(name: String, age: Int) {
    object MyJsonProtocol extends DefaultJsonProtocol {
        implicit object PersonFormat extends RootJsonFormat[User] {
            def write(u: User) = JsArray(JsString(u.name), JsNumber(u.age))
            def read(value: JsValue) = value match {
              case JsArray(Vector(JsString(name), JsNumber(age))) => new User(name, age.toInt)
              case _ =>  throw new DeserializationException("User expected")
            }
        }
    }
}
