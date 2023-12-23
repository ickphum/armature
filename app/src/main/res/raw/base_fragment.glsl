precision mediump float;
uniform float u_Time;
vec3 colorA = vec3(0.0,0.4,0.0);
vec3 colorB = vec3(0.0,0.2,0.0);

void main()
{
    vec3 color = vec3(0.0);

//    float pct = abs(sin(u_Time * 1.0));
    float pct = ((sin(u_Time * 3.0) + 1.0) / 2.0);

    // Mix uses pct (a value from 0-1) to
    // mix the two colors
    color = mix(colorA, colorB, pct);

    gl_FragColor = vec4( color, 1.0 );
}
