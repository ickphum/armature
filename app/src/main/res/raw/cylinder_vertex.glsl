#version 310 es
uniform mat4 u_Matrix;
uniform vec3 u_VectorToLight;
uniform vec4 u_Color;

layout (location=0) in vec3 a_Position;
layout (location=1) in vec3 a_Normal;
out vec4 v_Color;
void main()
{
    v_Color = u_Color;

    float diffuse = max(dot(a_Normal, u_VectorToLight), 0.0);
    v_Color *= diffuse;
    v_Color += 0.3; // ambient
    v_Color[3] = u_Color[3];

    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
