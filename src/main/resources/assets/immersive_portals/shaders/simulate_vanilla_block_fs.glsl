#version 120

//simulate vanilla fixed pipeline block rendering with portal culling

varying vec2 uv;
varying vec2 uv2;
varying vec3 pos;
varying vec4 color;
varying float fogDistance;

uniform sampler2D sampler;
uniform sampler2D sampler2;

uniform vec3 portalCenter;
uniform vec3 portalNormal;
uniform vec3 fogColor;
uniform float fogStart;
uniform float fogEnd;

void main(){
    vec3 realPos = pos;
    if (dot(realPos - portalCenter, portalNormal) > 0){
        discard;
    }

    vec4 tex = texture2D(sampler, uv);
    vec4 light=texture2D(sampler2, uv2);

    vec4 texWithColor = vec4(tex.x * color.x, tex.y * color.y, tex.z * color.z, tex.w * color.w);

    vec4 lighted=vec4(
    texWithColor.x * light.x,
    texWithColor.y * light.y,
    texWithColor.z * light.z,
    texWithColor.w
    );

    float fogFactor = (fogEnd - fogDistance) / (fogEnd-fogStart);
    fogFactor = clamp(fogFactor, 0, 1);

    gl_FragColor = lighted * fogFactor + vec4(fogColor, lighted.w) * (1 - fogFactor);
}