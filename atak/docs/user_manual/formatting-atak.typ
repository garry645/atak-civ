#import "@preview/polylux:0.4.0": *

#let accent_color = rgb(227, 119, 18)


#let maybe-read(path) = context {
  let path-label = label(path)
   let first-time = query((context {}).func()).len() == 0
   if first-time or query(path-label).len() > 0 {
    read(path)
  } else {
    rect(width: 50%, height: 5em, fill: luma(235), stroke: 1pt)[
      #set align(center + horizon)
      Could not find #raw(path)
    ]
  }
}

#let tak-slide(title: [], body) = { 

slide({
    set text(font: "Latin Modern Sans")

    show heading: it => {
      toolbox.register-section(it.body)
      line(length: 100%, stroke: accent_color)
      underline(it.body)
    }

    show heading: set text(accent_color)
show heading: it => {
      toolbox.register-section(it.body)
      line(length: 100%, stroke: accent_color)
 
      set text(accent_color)
      underline(
        if (it.level == 1) {
         set text(size: 13pt) 
          upper(it.body)
        } else {
          it.body
        }
      )
    }
    body
  })
}


#let tak-title-slide(
  version: [], 
 ) = { 
slide({
    set page(
      background: image("userguide.svg", width: 100%),
    )

    set text(font: "Libre Franklin")
    set align(center + horizon)
    set text(font: "Latin Modern Sans", tracking: 0pt)
    
    text(size: 32pt, "ATAK")
    text("\n\n")

    text("Software User Manual")
    text("\n")

    text(size: 1em, "Version: ")
    
    text(version)
    text("\n")

    datetime.today().display("[day] [month repr:long] [year]")
  })
}


#let contents-slide() = {
  set align(center)
  set text(font: "Latin Modern Sans")
  
  set page(header: 
    underline(stroke: accent_color, strong(text(size: 24pt, "Contents")))
  )
show outline.entry.where(level: 1): it => {
    set text(weight: "bold")
    upper(it)
  }
  toolbox.side-by-side(columns: (1fr, 6fr, 1fr))[][
    #outline(title: "")
  ][]
}

#let userguide(
  version: [], 
  doc) = [

  #set page(paper: "presentation-16-9")

  #set par(justify: true)

  #tak-title-slide(
    version: version,
  )

  #contents-slide()

  #set page(
    footer: context [
      #set align(right)
      #set text(10pt)
      #counter(page).display(
        "1 / 1",
        both: true,
      )
    ]
  )

  #doc
]
