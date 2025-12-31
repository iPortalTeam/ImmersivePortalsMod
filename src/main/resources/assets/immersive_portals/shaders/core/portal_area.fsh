#version 330

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    fragColor = color;
}
