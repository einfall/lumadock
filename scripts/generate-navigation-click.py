"""Original, procedurally synthesized Luma Dock click; no external sound sample."""
from pathlib import Path
import math,struct,wave
target=Path('launcher/app/src/main/res/raw/navigation_click.wav')
rate=44100;frames=[]
for i in range(round(rate*.04)):
 t=i/rate
 attack=min(1,t/.0004)
 envelope=attack*math.exp(-t/0.006)*(0.5+0.5*math.cos(math.pi*t/.04))
 signal=.5*envelope*(.74*math.sin(2*math.pi*1600*t)+.26*math.sin(2*math.pi*3100*t))
 frames.append(struct.pack('<h',round(32767*signal)))
with wave.open(str(target),'wb') as output:
 output.setnchannels(1);output.setsampwidth(2);output.setframerate(rate);output.writeframes(b''.join(frames))
print(target,target.stat().st_size)
