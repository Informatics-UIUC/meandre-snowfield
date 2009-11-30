import org.scalacheck._

/** This object implement the main entry point for the specification 
 * driven test for the Meandre Infrastructure.
 *
 * @author Xavier
 */
object Specification extends Properties("String") {
  
  property("startsWith") = Prop.forAll((a: String, b: String) => (a+b).startsWith(a))

  property("endsWith") = Prop.forAll((a: String, b: String) => (a+b).endsWith(b))

  property("concat") = Prop.forAll((a: String, b: String) => 
    (a+b).length >= a.length && (a+b).length >= b.length
  )

  property("substring") = Prop.forAll((a: String, b: String) => 
    (a+b).substring(a.length) == b
  )

  property("substring") = Prop.forAll((a: String, b: String, c: String) =>
    (a+b+c).substring(a.length, a.length+b.length) == b
  )

}
