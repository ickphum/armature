#version 310 es
uniform mat4 u_Matrix;

layout ( location=0 ) in vec3 a_Position;
void main()
{
    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
