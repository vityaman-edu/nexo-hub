import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ApplicationKtTest {
    @Test
    fun testMain() {
        assertDoesNotThrow(::main)
    }
}