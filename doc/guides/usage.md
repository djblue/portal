# Usage

A lot of this will come down to your personal preference, but I like to work similarly to this:

Say for example that we have a function that is misbehavin' and we don't understand how or why
```Clojure
(defn very-good-function
  “This is the best code I’ve ever written!”
  [param1 param2]
  (let [bound1 (foo param1 param2)
         bound2 (this-is-a-kanye-west-reference param2)]
    (tap> {:param1 param1
              :param2 param2
              :bound1 bound1})
    (other-code bound1 bound2)))
```

That's pretty nice function, eh? Let's debug it.
First, notice the usage of `tap>` above. It's being passed a map of key value pairs. This isn't necessary, but I find it to be extremely useful at taking as much guesswork out of the debugging process as possible.

Next, select the data that you'd like to access in the Portal UI.
Assuming that you've `def`ed the Portal object to the `dev` namespace, you can dereference it with @dev/portal.

Note: If you aren't familiar with how to bind your instance of Portal to a namespace, see `../editors/emacs.md` or `../editors.vscode.md` for more information.

I quite like using rich comment blocks here. They allow me to poke at very specific bits of code with real data of my choosing.

```Clojure
(comment
  (let [{:keys [param1 param2 bound1] @dev/portal}]
    (this-is-a-kanye-west-reference param2)))
```

In this example, we may notice that the call to `this-is-a-kanye-west-reference` produces results that aren't what we expect.
We can then go further down that rabbit hole and apply the same process.