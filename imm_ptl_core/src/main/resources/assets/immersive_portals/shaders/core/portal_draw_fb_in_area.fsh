#version 150

uniform sampler2D DiffuseSampler;

uniform float w;
uniform float h;

out vec4 fragColor;

void main() {
    //screen space from -1 to 1
    //texture from 0 to 1

    fragColor = texture(DiffuseSampler, vec2(gl_FragCoord.x/w, gl_FragCoord.y/h));

    //vec4 sampled=texture2D(sampler, vec2(gl_FragCoord.x/w, gl_FragCoord.y/h));
    //gl_FragColor = vec4(1.0,sampled.yz,1.0);
}