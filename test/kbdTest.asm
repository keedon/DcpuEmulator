; Keyboard test
:start	set i,0x8000		; Start of screen
		set j,0				; index into keyboard buffer
:loop
		set a,[0x9000 + j]	; Key
		ife	a,0
		set	pc,loop
		add j,1
		and j,0xf
		set	[i],a
		add i,1
		set pc,loop		