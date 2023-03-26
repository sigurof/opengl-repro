#version 420 core
layout (location = 0) in vec3 vtxPosition;
uniform vec2 position;

void main() {
    vec2 vertexPos = vec2(vtxPosition.x, vtxPosition.y);
    vec2 outputPos = vertexPos + position;
    gl_Position = vec4(outputPos, 0f, 1f);
}