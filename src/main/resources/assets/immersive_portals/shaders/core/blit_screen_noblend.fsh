#version 330

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // change alpha to 1.0
    fragColor = vec4(texture(DiffuseSampler, texCoord).xyz, 1.0f);
}
