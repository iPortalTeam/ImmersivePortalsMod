#version 330

uniform sampler2D DiffuseSampler;

layout(std140) uniform IPortalParams {
    vec4 iportal_FbSize;
};

out vec4 fragColor;

void main() {
    //screen space from -1 to 1
    //texture from 0 to 1

    vec2 fbSize = iportal_FbSize.xy;
    fragColor = texture(DiffuseSampler, vec2(gl_FragCoord.x / fbSize.x, gl_FragCoord.y / fbSize.y));

    //vec4 sampled=texture2D(sampler, vec2(gl_FragCoord.x/w, gl_FragCoord.y/h));
    //gl_FragColor = vec4(1.0,sampled.yz,1.0);
}
