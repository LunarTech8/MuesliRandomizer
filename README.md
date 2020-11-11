# Muesli Randomizer
 
App to generate randomized muesli mixes. Mueslis are picked from an exhaustible, recurring list based on given total size, sugar percentage and number of items. An availibility multiplier (0x-3x) for each muesli can be defined via a separate table. Results are displayed in convenient regular tablespoons.

New mixes are generated and evaluated until a close enough approximation to the given settings is found. To be able to better reach low sugar percentages a random filler muesli with a very low sugar amount is added as last item. When there aren't enough mueslis left in the list to get a valid mix the remnants are marked as fixed and the list is repopulated with the used items. Thus all mueslis are used evenly according to their availibility multipliers.

<table>
  <tr>
    <td>Regular view</td>
    <td>Availibility multiplier table</td>
  </tr>
  <tr>
    <td><img src="app/src/main/pics/Showcase01.jpg" width=384></td>
    <td><img src="app/src/main/pics/Showcase02.jpg" width=384></td>
  </tr>
</table>
