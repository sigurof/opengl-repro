import Shader.compileShaderFromSource
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import kotlin.math.cos

const val WIDTH = 1280
const val HEIGHT = 720

private val quadVertices: FloatArray = arrayOf(
    0.1f, 0.1f, 0.0f,  // top right
    0.1f, -0.1f, 0.0f,  // bottom right
    -0.1f, -0.1f, 0.0f,  // bottom left
    -0.1f, 0.1f, 0.0f   // top left
).toFloatArray()
private val quadIndices = arrayOf(
    0, 1, 3,
    1, 2, 3
).toIntArray()

fun main() {

    // Initialise GLFW and OpenGL
    GLFWErrorCallback.createPrint(System.err).set();
    if (!GLFW.glfwInit())
        throw IllegalStateException("Unable to initialize GLFW");
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL20.GL_TRUE)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)

    val window = GLFW.glfwCreateWindow(
        WIDTH,
        HEIGHT, "Hello Window", 0, 0
    )
    GLFW.glfwMakeContextCurrent(window)
    GLFW.glfwShowWindow(window)
    createCapabilities()

    // Send Quad data to Graphics card
    val vao = glGenVertexArrays()
    ARBVertexArrayObject.glBindVertexArray(vao)
    val vbo = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_DYNAMIC_DRAW)
    val ebo = glGenBuffers()
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL_DYNAMIC_DRAW)
    GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
    GL20.glEnableVertexAttribArray(0) // says the vertex attribute location used in the shader
    ARBVertexArrayObject.glBindVertexArray(0) // unbind vao

    // Compile shader
    val shaderProgram = compileProgram("vertex.shader", "fragment.shader", listOf())
    val positionUniformLocation = GL20.glGetUniformLocation(shaderProgram, "position")
    val colorUniformLocation = GL20.glGetUniformLocation(shaderProgram, "color")

    val startTime = System.currentTimeMillis()
    var lastFrameTime = System.currentTimeMillis()
    val timePerFrame = (1000.0 / 60).toLong()

    // Render 60fps
    while (!glfwWindowShouldClose(window)) {
        val timeSinceLastFrame = System.currentTimeMillis() - lastFrameTime
        if (timeSinceLastFrame > timePerFrame) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glClearColor(0.23f, 0.5f, 0.1f, 0f)

            // Activate shader, quad
            glUseProgram(shaderProgram)
            glBindVertexArray(vao)
            val currentTimeMillis = System.currentTimeMillis()
            val timeValue = currentTimeMillis - startTime
            val xPosition = 0.9f * cos(timeValue.toFloat() / 1000)
            GL20.glUniform2f(positionUniformLocation, xPosition, 0f)
            GL20.glUniform4f(colorUniformLocation, 1f, 0f, 0f, 1f)
            glDrawElements(GL_TRIANGLES, quadIndices.size, GL_UNSIGNED_INT, 0)
            glBindVertexArray(0)
            glUseProgram(0)

            GLFW.glfwSwapBuffers(window) // swap the color buffers
            lastFrameTime = System.currentTimeMillis()
        }
        GLFW.glfwPollEvents()
    }
}

fun compileProgram(vtxSource: String, frgSource: String, attributes: List<Pair<Int, String>>): Int {
    val program = GL20.glCreateProgram()
    val vtxShader: Int =
        compileShaderFromSource(
            vtxSource,
            GL20.GL_VERTEX_SHADER
        )
    val frgShader: Int =
        compileShaderFromSource(
            frgSource,
            GL20.GL_FRAGMENT_SHADER
        )
    GL20.glAttachShader(program, vtxShader)
    GL20.glAttachShader(program, frgShader)
    for (attribute in attributes) {
        GL20.glBindAttribLocation(program, attribute.first, attribute.second)
    }
    GL20.glLinkProgram(program)
    if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
        val info = GL20.glGetProgramInfoLog(program, 512)
        throw IllegalStateException("Failed to compile shader program:\n $info")
    }
    GL20.glDetachShader(program, vtxShader)
    GL20.glDetachShader(program, frgShader)
    GL20.glDeleteShader(vtxShader)
    GL20.glDeleteShader(frgShader)
    return program
}

object Shader {

    fun compileShaderFromSource(source: String, typeGl: Int): Int {
        val text = (Shader::class.java.getResource(source) ?: error("Failed to read '$source'.")).readText()
        val shader: Int = GL20.glCreateShader(typeGl)
        GL20.glShaderSource(shader, text)
        GL20.glCompileShader(shader)
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            val shaderInfoLog = GL20.glGetShaderInfoLog(shader, 512)
            val shaderType = when (typeGl) {
                GL20.GL_VERTEX_SHADER -> "vertex"
                GL20.GL_FRAGMENT_SHADER -> "fragment"
                else -> "unknown"
            }
            throw IllegalStateException("Failed to compile $shaderType shader from source: \n Source: '$source',\n OpenGL shader info log: $shaderInfoLog")
        }
        return shader
    }
}
