#version 310 es
precision mediump float;
uniform samplerCube u_TextureUnit;
in vec3 v_Position;
out vec4 finalColor;
void main()
{
    finalColor = texture(u_TextureUnit, v_Position);
}