#version 120

uniform sampler2D sampler;
uniform float w;
uniform float h;

void main(){
    //screen space from -1 to 1
    //texture from 0 to 1

    //    gl_FragColor = vec4(
    //    texture2D(
    //    sampler,
    //    vec2(gl_FragCoord.x/w, gl_FragCoord.y/h)
    //    ).xyz,
    //    1
    //    );
    gl_FragColor = texture2D(sampler, vec2(gl_FragCoord.x/w, gl_FragCoord.y/h));
}