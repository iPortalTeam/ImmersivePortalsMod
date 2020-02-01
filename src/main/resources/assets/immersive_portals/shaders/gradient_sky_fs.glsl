#version 120

varying vec3 pos;
varying vec4 color;

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