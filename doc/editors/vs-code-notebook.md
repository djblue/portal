# Clojure Notebooks

If you are interested in using Portal as a renderer for [Calva's Clojure
Notebooks][1], you've come to the right place.

Before getting started, make sure you have both the [Calva][2] and [Portal][3] VS Code extensions installed.

## Usage

<p align="center">
<img src="https://user-images.githubusercontent.com/1986211/196565058-dd6a1bfb-f27d-498a-926e-9758d9cc0b4e.gif" />
</p>

With the above extensions installed, you should now be able to open any Clojure
file by right clicking the file in the file explorer and selecting `Open
With...` and selecting `Clojure Notebook`. The output cells have a three dot
menu where you can change the presentation to Portal!

> **Note**
> Calva gives Portal the evaluation results as an edn string therefore
> this mode of Portal will not be the same as a process connected Portal
> instance.

[1]: https://calva.io/notebooks/
[2]: https://marketplace.visualstudio.com/items?itemName=betterthantomorrow.calva
[3]: https://marketplace.visualstudio.com/items?itemName=djblue.portal

## `portal.nrepl/wrap-notebook`

If you would like the same facilities provided by a process connected Portal
instance directly in your Calva notebooks, simply add the
`portal.nrepl/wrap-notebook` middleware to your nrepl stack. This will enable:

- [Datafy + Nav](../datafy.md)
- First class object support
- Runtime registered commands
- Larger values which are harder for edn to serialize and parse
- Better error rendering
