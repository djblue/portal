Launch the demo with 

    clj -Adev

Once launched eval the lines below which are located in dev/user.clj to open the portal window, I am not sure if you can make figwheel eval the contents of user.cljs on launch.

    (def portal (p/open))
    (p/tap)

