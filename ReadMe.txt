/*  Design of the Mobile App is intended for the course: mobile App for Internet of Things at CMU SV
 *  , which incooperates the control of Philip Hue and associative features
 */

1. The repository support mainly three modes for smart bulb.
a. Norm: Users can arbitrary tune the intensity of the bulb through seekBar
b. Auto: Bulb will be turned on as long as the intensity is lower than threshold (100 lu) 
c. Alarm: User can set the timer through "SET" button and confirmation to update the internal time counter
          The bulb will be turned on as the time is up. The timer can support as to hour = 23, min = 60, sec = 60
          If the input is valid, this App would like to warn you to reenter the counter