uniform mat4 u_Matrix;
//uniform float u_Time;

attribute vec3 a_Position;
void main()
{
//    float pct = ((sin(u_Time * 3.0) + 1.0) / 2.0);
//    vec3  tempPos = a_Position;
//    tempPos.y += pct;
//    gl_Position = u_Matrix * vec4(tempPos, 1.0);
    gl_Position = u_Matrix * vec4(a_Position, 1.0);
}
